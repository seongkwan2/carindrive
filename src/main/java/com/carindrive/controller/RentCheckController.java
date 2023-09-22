package com.carindrive.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.carindrive.CarPwdCh;
import com.carindrive.service.OrderService;
import com.carindrive.service.RentService;
import com.carindrive.vo.CarVO;
import com.carindrive.vo.MemberVO;
import com.carindrive.vo.OrderVO;
import com.carindrive.vo.RentalVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/rent/*")
public class RentCheckController {

	@Autowired
	private RentService rentService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private static final Logger log = LoggerFactory.getLogger(RentCheckController.class);

	//렌탈 정보 저장
	@RequestMapping("/rent_Check")//rentOK.jsp에서 넘어온 데이터
	public ResponseEntity<Map<String, Object>> rent_Check(@RequestBody OrderVO order,HttpSession session) {

		Map<String, Object> map = new HashMap<>();

		try {
			// 로그인 정보 가져오기
			MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
			// 해당 고객의 렌탈 정보 가져오기
			RentalVO rental = this.rentService.getRentOne(memberInfo.getM_id());
			// 해당 렌탈정보에 예약번호에 맞는 주문번호 추가
			String merchantId = order.getMerchantId();
			this.rentService.insertMerchantId(merchantId, rental.getCr_num());

			// 결제정보 getPayInfo 메서드에 주문번호를 넣고 OrderVO에 값들을 셋팅
			OrderVO orderInfo = getPayInfo(merchantId);

			// 데이터베이스에 OrderVO 결제정보 저장	//setBuy_date만 저장해서 넣어야될수도있다.
			this.orderService.saveOrder(orderInfo);

			map.put("orderInfo", orderInfo);
			map.put("rental", rental);
			map.put("success", true);
			map.put("redirectUrl", "/rent/rent_Check_List"); // 리디렉트할 URL 추가

			return new ResponseEntity<>(map, HttpStatus.OK);

		} catch (Exception e) {//결제시 문제 발생
			try {
				// 환불 처리 시작
				String token = getImportToken();

				if (token == null || token.isEmpty()) {
					log.error("인증 정보에 문제가 발생했습니다. 환불 처리를 위해 다시 시도해주세요.");
				} else {
					double refundAmount = order.getAmount(); // 주문에서 환불 금액을 가져옴
					int result_delete = cancelPay(token, order.getMerchantId(), refundAmount); 

					if (result_delete == -1) {
						log.error("환불에 실패했습니다. 다시 시도해주세요.");
					} else {
						this.orderService.refundOK(order.getMerchantId()); // 환불 완료시 refund 업데이트
						log.info("환불이 완료되었습니다.");
					}
				}// 환불 처리 종료

			} catch (Exception refundException) {
				log.error("환불 처리 중 오류 발생: ", refundException);

				// 경고창을 띄우고 서비스 센터로 리디렉션
				map.put("redirectUrl", "/service/service_center");
				map.put("message", "환불 처리 중 오류가 발생했습니다. 1:1 문의를 통해 문제를 알려주세요.");
				map.put("success", false);

				return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			map.put("success", false);
			map.put("message", "결제 정보 처리 중 오류 발생 컨트롤러");
			log.error("결제 정보 가져오기 오류: ", e);
			return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	//예약 확인
	@RequestMapping(value = "/rent_Check_List")
	public ModelAndView rent_Check_List(HttpSession session, RedirectAttributes rttr) {
		ModelAndView mav = new ModelAndView();
		MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

		try {
			if (memberInfo != null) {//로그인시
				List<RentalVO> rentals = this.rentService.getRentList(memberInfo.getM_id());//아이디를 기준으로 렌트정보를 다가져옴
				Map<String, RentalVO> rentalMap = new HashMap<>(); 	//키,값으로 담기위해 map 생성
				for (RentalVO rental : rentals) {					//(RentalVO)rentals를 rental로 한개씩 뽑아서 키,값으로 저장
					rentalMap.put(rental.getCr_order(), rental);	//주문번호, 렌탈정보로 저장시킴 주문번호 호출시 렌탈정보 전체가 호출됨
				}

				mav.addObject("rentalMap", rentalMap);
				mav.addObject("rentals", rentals);

				List<OrderVO> orders = this.orderService.getCashInfo(memberInfo.getM_id());
				List<OrderVO> orderInfos = new ArrayList<>();

				List<CarVO> carInfos = new ArrayList<>(); 


				for (OrderVO order : orders) {
					OrderVO orderInfo = orderService.getOrder(order.getId());
					orderInfos.add(orderInfo);

				}

				//정렬
				Collections.sort(orderInfos, Comparator.comparing(OrderVO::getBuy_date).reversed());

				for (OrderVO order : orderInfos) {
					CarVO carInfo = null;
					//차량 정보를 불러오는 mybatis문 orders에 들어있는 차량 이름으로 검색

					String[] parts = order.getBuy_product_name().split(" ");
					String carName = parts[parts.length - 1];

					carInfo = this.rentService.getCarInfo(carName);
					carInfos.add(carInfo);
				}

				mav.addObject("memberInfo", memberInfo);
				mav.addObject("carInfos", carInfos);
				mav.addObject("orderInfos", orderInfos);
			} else {
				rttr.addFlashAttribute("LoginNull", "로그인 이후 이용 가능합니다!");
				mav.setViewName("redirect:/member/m_login");
				return mav;
			}

			mav.setViewName("/rent/rent_Check_List");
			return mav;
		} catch (Exception e) {
			e.printStackTrace();
			mav.setViewName("/rent/rent_Check_List");
			return mav;
		}
	}

	//예약내역 상세보기
	@RequestMapping(value="/rent_details")
	public ModelAndView rent_details(@RequestParam("merchantId") String merchantId,
			@RequestParam("carname") String carName, HttpSession session) {
		ModelAndView model = new ModelAndView();
		//로그인정보
		MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
		//차량 정보 가져오기
		CarVO carInfo = this.rentService.getCarInfo(carName);
		//렌탈 정보 가져오기
		RentalVO rentalInfo = this.rentService.getRentCar(merchantId);
		//결제내역
		OrderVO orderInfo = this.orderService.getOrder2(merchantId);

		model.addObject("memberInfo", memberInfo);
		model.addObject("carInfo",carInfo);
		model.addObject("rentalInfo",rentalInfo);
		model.addObject("orderInfo",orderInfo);

		return model;
	}


	//환불 관련 메서드

	
	//본인 인증 메서드 //사용안함
	@RequestMapping(value="refund_Check")
	public ModelAndView refund_Check(HttpSession session, HttpServletRequest request) throws Exception {
		
		System.out.println("refund_Check 메서드 동작");
		ModelAndView model = new ModelAndView();
		try {
		MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo"); //로그인정보

		String mPwd = request.getParameter("mPwd");
		System.out.println("암호화 전: "+mPwd);

		if(mPwd != null) {//사용자에게 값을 입력 받은 뒤
			mPwd = CarPwdCh.getPassWordToXEMD5String(mPwd);
			System.out.println("암호화 후: "+mPwd);
			if(memberInfo.getM_pwd().equals(mPwd)) {//인증이 되면
				System.out.println("인증완료");
			}else {
				System.out.println("인증실패");
			}
		}//
		return model;
		}catch(Exception e) {
			e.printStackTrace();
			model.addObject("noSession", true);//세션이 없음을 jsp에 전달 //이 기능이 동작을 안함
			return model;
		}
	};





	//토큰을 받아오는 코드
	// IAMPORT의 API 키와 시크릿
	private static final String API_KEY = "6723566850304883";
	private static final String API_SECRET = "0HPz5ReT4GqEPChHkVJas7cdWUdBhlXrFY6Ny9YrB6J3Q9ad9zpfzeMSlZx260IcSFIGnLfxWYSkw3By";
	public static final String IMPORT_PAYMENTINFO_URL = "https://api.iamport.kr/payments/find/";

	//토큰 얻어오기
	private String getImportToken() throws Exception {
		String tokenUrl = "https://api.iamport.kr/users/getToken";
		RestTemplate restTemplate = new RestTemplate();

		Map<String, String> params = new HashMap<>();
		params.put("imp_key", API_KEY);
		params.put("imp_secret", API_SECRET);

		// HttpEntity 객체를 생성하여 본문과 헤더를 함께 전달합니다.
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

		if (response.getStatusCode() == HttpStatus.OK && response.getBody().get("code").equals(0)) {
			Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("response");
			String accessToken = (String) responseData.get("access_token");

			// 얻은 토큰을 콘솔에 출력
			System.out.println("IAMPORT Access Token: " + accessToken);

			return accessToken;
		} else {
			throw new Exception("IAMPORT token request failed: " + response.getBody().get("message"));
		}
	}

	// 결제정보 조회 메서드
	public OrderVO getPayInfo(String merchantId) throws Exception { 
		String buyer_name = "";
		String buyer_phone = "";
		String member_email = "";
		String buyer_addrStr = "";
		String buyer_postcode = "";
		String buyer_addr = "";
		String paid_at = "";
		String buy_product_name = "";
		String buyer_buyid = "";
		String buyer_merid = "";
		String amount = "";
		String buyer_card_num = "";
		String buyer_pay_ok = "";
		long buyer_pay_price = 0L;
		long paid_atLong = 0L;
		long unixTime = 0L;
		Date date = null;

		String token = getImportToken();	//토큰생성 API요청 인증에 활용

		//HttpClient를 사용하여 API에게 요청을 보냄
		HttpClient client = HttpClientBuilder.create().build(); 
		HttpGet get = new HttpGet(IMPORT_PAYMENTINFO_URL + merchantId + "/paid"); //merchantId를 보낸 이유
		get.setHeader("Authorization", token); 
		try { 
			HttpResponse res = client.execute(get);
			ObjectMapper mapper = new ObjectMapper(); 
			String body = EntityUtils.toString(res.getEntity()); 
			JsonNode rootNode = mapper.readTree(body); 
			JsonNode resNode = rootNode.get("response"); 
			log.info("wowwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww resNode: "+resNode);

			// API에게 받은 응답은 JsonNode방식으로 파싱됨
			///JsonNode에 저장되어 있는 값들을 변수에 저장
			buyer_name = resNode.get("buyer_name").asText(); 
			buyer_phone = resNode.get("buyer_tel").asText(); 
			member_email = resNode.get("buyer_email").asText(); 

			buyer_addrStr = resNode.get("buyer_addr").asText(); 
			buyer_postcode = resNode.get("buyer_postcode").asText(); 
			buyer_addr = buyer_addrStr+" "+buyer_postcode; //주소에 우편번호 합치기

			paid_at = resNode.get("paid_at").asText(); //결제시간
			buy_product_name = resNode.get("name").asText(); 
			buyer_buyid = resNode.get("imp_uid").asText(); 
			buyer_merid = resNode.get("merchant_uid").asText(); 
			amount = resNode.get("amount").asText();
			buyer_card_num = resNode.get("apply_num").asText(); 
			buyer_pay_ok = resNode.get("status").asText(); 
			//buy_date = resNode.get("buy_date").asText(); 



			log.info("++++++++++++++++++++++++++++++++++++import buyer_name: "+buyer_name);
			log.info("++++++++++++++++++++++++++++++++++++import paid_at: "+paid_at);

		} catch (Exception e) { 
			e.printStackTrace(); 
		} 

		buyer_pay_price = Long.parseLong(amount);

		// 결제 시간 - 형식 바꾸기
		paid_atLong = Long.parseLong(paid_at);
		unixTime = paid_atLong * 1000;
		date = new Date(unixTime);

		// 형식 바꾸기
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+9")); // GMT(그리니치 표준시 +9 시가 한국의 표준시

		String buy_date = sdf.format(date);	//paid_at를 이용해서 생성된 buy_date
		log.info("++++++++++++++++++++++++++++++++++++import date: "+buy_date);


		OrderVO order_info = new OrderVO(-1L, buyer_name, buyer_phone, member_email, 
				buyer_addr, buy_date, buy_product_name, buyer_buyid, buyer_merid, 
				buyer_pay_price, buyer_card_num, buyer_pay_ok, -1);

		return order_info;
	}

	//환불하기 기능
	@PostMapping("/refund")
	public void refund(@RequestParam String order_number, HttpServletResponse response, HttpSession session) throws Exception {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		try {
			// 로그인 고객정보 가져오기
			MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

			// 해당 아이디의 모든 렌트 정보를 가져옴
			List<RentalVO> rentals = this.rentService.getRentList(memberInfo.getM_id());

			// 리스트에 담긴 모든 렌트 정보에서 주문번호로 예약 내역 찾기
			RentalVO rentalRefund = null;
			for (RentalVO rental : rentals) {
				if (order_number.equals(rental.getCr_order())) {//RentalVO에 들어있는 주문번호와 order_number가 
					rentalRefund = rental;						//동일할때 까지 돌려서 어떤걸 환불할때 주문번호로 사용할것인지 찾음
					break;										//rentalRefund에 환불할 정보들이 담김
				}
			}

			if (rentalRefund == null) {
				out.print("일치하는 주문번호를 찾을 수 없습니다.");
				out.close();
				return;
			}

			// String 날짜를 LocalDateTime으로 변환
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			LocalDateTime rentalDateTime = LocalDateTime.parse(rentalRefund.getCr_sdate(), formatter);

			// 하루 전 및 이틀 전 날짜 계산
			LocalDateTime oneDayBeforeRental = rentalDateTime.minusDays(1);
			LocalDateTime twoDaysBeforeRental = rentalDateTime.minusDays(2);

			// 현재 날짜와 시간
			LocalDateTime now = LocalDateTime.now();

			String token = getImportToken();

			if (token == null || token.isEmpty()) {
				alertMessage(out, "인증 정보에 문제가 발생했습니다 다시 시도해주세요!");
				return;
			}

			double refundAmount;

			// 하루 전 환불 불가능
			if (now.isAfter(oneDayBeforeRental)) {
				out.println("<script>");
				out.println("alert('대여시간 24시간 이내는 환불이 불가능합니다.');");
				out.println("window.close()");	//현재창을 닫고
				out.println("</script>");
			}
			// 이틀 전 환불
			else if (now.isAfter(twoDaysBeforeRental) && now.isBefore(oneDayBeforeRental)) {	
				refundAmount = rentalRefund.getCr_price() * 0.5; //환불처리후 새로고침
				processRefund(token, order_number, refundAmount, out, "환불 처리 되었으나, 환불 금액은 50%입니다.");
			}
			// 그 외 환불 금액 100% 가능
			else {
				refundAmount = rentalRefund.getCr_price(); //환불처리후 새로고침
				processRefund(token, order_number, refundAmount, out, "환불이 완료 되었습니다!");
			}
			out.close();

		}catch (Exception e) {
			out.println("<script>");
			out.println("alert('세션이 만료되었습니다 다시 로그인해주세요!');");
			out.println("window.open('/member/m_login', '_blank');");//로그인창으로 이동
			out.println("</script>");
		} finally {
			out.close();
		}
	}

	//반복되는 자바스크립트 코드 메서드화
	private void alertMessage(PrintWriter out, String message) {
		out.println("<script>");
		out.println("alert('" + message + "');");
		out.println("window.close()");	//현재창을 닫고
		out.println("opener.location.reload();"); //부모창을 새로고침
		out.println("</script>");
	}

	//반복되는 환불처리 메서드
	private void processRefund(String token, String order_number, double refundAmount, PrintWriter out, String successMessage) {
		int result_delete = cancelPay(token, order_number, refundAmount);
		if (result_delete == -1) {
			alertMessage(out, "환불에 실패 했습니다 다시 시도해주세요!");
		} else {
			this.orderService.refundOK(order_number); // 환불완료 시 refund에 '환불완료' 업데이트
			alertMessage(out, successMessage);
		}
	}


	//환불 API 구현
	public static final String IMPORT_CANCEL_URL = "https://api.iamport.kr/payments/cancel"; 

	public int cancelPay(String token, String order_number, double refundAmount) { 
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(IMPORT_CANCEL_URL); 
		Map<String, String> map = new HashMap<String, String>(); 
		post.setHeader("Authorization", token); 
		map.put("merchant_uid", order_number);						//주문번호와
		map.put("amount", String.valueOf(refundAmount));	//환불금액 정보 추가

		try { 
			post.setEntity(new UrlEncodedFormEntity(convertParameter(map), "UTF-8")); 
			CloseableHttpResponse res = client.execute(post); 

			if (res.getStatusLine().getStatusCode() != 200) {
				System.err.println("환불 요청 실패: " + res.getStatusLine().getReasonPhrase());
				return -1;
			}

			String responseBody = EntityUtils.toString(res.getEntity());
			ObjectMapper mapper = new ObjectMapper(); 
			JsonNode rootNode = mapper.readTree(responseBody);

			if (rootNode.has("response") && !rootNode.get("response").isNull()) {
				System.out.println("환불성공");
				return 1;
			} else {
				System.err.println("환불실패");
				return -1;
			}

		} catch (Exception e) {
			e.printStackTrace(); 
			System.err.println("환불 요청 중 오류 발생");
			return -1;
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<NameValuePair> convertParameter(Map<String, String> params) {
		List<NameValuePair> paramList = new ArrayList<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		return paramList;
	}
}