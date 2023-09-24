<%@ page contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<%
    String calculatedPrice = request.getParameter("calculatedPrice");
    if (calculatedPrice == null || calculatedPrice.isEmpty()) {
        calculatedPrice = "0";
    }
    request.setAttribute("calculatedPrice", calculatedPrice);
%>
<html>
<head>
	<script src="${path}/js/jquery.js"></script>
    <script type="text/javascript" src="https://cdn.iamport.kr/js/iamport.payment-1.2.0.js"></script>
<meta charset="UTF-8">
<title>시간 연장</title>
</head>
<script>
//전역 변수로 buy_date 선언 결제일자 새로갱신
let globalBuyDate = new Date().getTime();
function payMent(paymentType, rental_cr_mid, car_c_name, car_c_color, car_c_year, calculatedPrice, order) {
	console.log("Order:", order);


	if (typeof calculatedPrice === 'undefined' || !calculatedPrice) {
	    alert('결제 금액을 확인할 수 없습니다.');
	    return;
	}

	IMP.init('imp87360186');

	 var showName = car_c_year+'년식 ' + car_c_color + ' ' + car_c_name +' 시간연장'
	 var pgValue;
	 globalBuyDate = new Date().getTime();	// 날짜 값을 전역 변수에 저장

	    if (paymentType === "card") {
	        pgValue = "html5_inicis";
	    } else if (paymentType === "kakao") {
	        pgValue = "kakaopay";
	    }
	 //IMP.request_pay() 함수는 내부적으로 정의된 파라미터만 처리하므로, buy_date와 같은 '사용자 정의 파라미터는 무시'된다.
	 //그렇기 때문에 결제 후 콜백 함수 내부에서 rsp.buy_date를 참조하려고 하면 값이 없어서 null이 된다.
	 //buy_date를 '전역 변수로 설정'하여 payMent 함수에서 값을 할당하고, 콜백 함수 내에서 이 값을 사용하는 방식으로 수정
	 
	IMP.request_pay({
	    pg : pgValue,
	    pay_method : 'card', //카드결제
	    merchant_uid : 'merchant_' + new Date().getTime(),
	    name : showName,
	    amount : calculatedPrice, //판매가격
	    buyer_name : rental_cr_mid,
	}, function(rsp) {
	    if ( rsp.success ) {
		var msg = '결제가 완료되었습니다.';
		msg += '고유ID : ' + rsp.imp_uid;
		msg += '상점 거래ID : ' + rsp.merchant_uid;
		msg += '결제 금액 : ' + rsp.paid_amount;
		msg += '카드 승인번호 : ' + rsp.apply_num;
		   
		pay_info(rsp,order); //pay_info내부의 orderData의 정보들을 /rent/pay_Check로 보냄 
		
	    } else {
	        var errorMsg = '결제가 취소되었습니다.';
	        alert(errorMsg);
	        history.back();	//이전 페이지로 돌아가기
	    }
	});
}
//데이터 담아서 비동기식으로 JSON타입으로 데이터 전송
	function pay_info(rsp, order) {

    var orderData = {
        buyer_name: rsp.buyer_name,
        buyer_phone: rsp.buyer_tel,
        member_email: rsp.buyer_email,
        buy_addr: rsp.buyer_addr,
        buy_product_name: rsp.name,
        buyer_buyid: rsp.imp_uid,
        amount: rsp.paid_amount,
        buyer_card_num: rsp.apply_num,
        buyer_pay_ok: rsp.success,
        buyer_postcode: rsp.buyer_postcode,
        merchantId: rsp.merchant_uid,
        paid_at: globalBuyDate,
        parent_merchant_id: order,
        
    };

 // AJAX 요청
    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/rent/pay_Check",
        data: JSON.stringify(orderData),
        dataType: "json",
        success: function(map) {
            console.log(map); 
            console.log("Success value:", map.success);
            
            if(map.success) {
            	const orderInfo = map.orderInfo;
                const rental = map.rental;
                location.href = map.redirectUrl	 // 서버에서 지정한 URL로 리디렉트

        }else {
                alert("결제 정보 처리 중 오류 발생 다시 시도해주세요");
                history.back(); //이전 페이지로 이동
        }
    },
        error: function(error) {
        console.error("Error:", error); //콘솔에 에러 로그 출력
            alert("오류로 인해서 결제가 취소되었습니다. 다시 시도해주세요");
            location.href = "/rent/timeUpPay";
        }
	});
}
</script>
<body>

<br>
	결제창<br>
	결제인:${memberInfo.m_name}<br>
	차량 : ${car.c_name}<br>
	주문번호:${rental.cr_order}<br>
	결제가격:${calculatedPrice}<br>
	

<button onclick="payMent('card', '${rental.cr_mid}', '${car.c_name}', '${car.c_color}', '${car.c_year}', ${calculatedPrice}, '${rental.cr_order}')">카드 결제</button>
<button onclick="payMent('kakao', '${rental.cr_mid}', '${car.c_name}', '${car.c_color}', '${car.c_year}', ${calculatedPrice}, '${rental.cr_order}')">카카오페이 결제</button>


	
</body>
</html>