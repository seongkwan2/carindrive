<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="path" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
<script src="${path}/js/jquery.js"></script>
<script src="${path}/js/payment.js"></script> <!-- 결제 코드 js -->
<script type="text/javascript" src="https://cdn.iamport.kr/js/iamport.payment-1.2.0.js"></script>
<link href="${path}/css/main.css" rel="stylesheet"/>
<link href="${path}/css/rent.css" rel="stylesheet"/>
<link href="${path}/css/rent_Check.css" rel="stylesheet" />

</head>
<body>
  <jsp:include page="../include/header.jsp"/>
  
  <div class="clear"></div>
<%-- 예약 선택 메뉴 --%>
<div class="mainmenu">
<div id="menu01">
	<input type="checkbox" name="accordian" id="car01">
	<label for="car01">2023.07.17 | 09:00</label>
	<div><p><input type="date"></p></div>
</div>
<div  id="menu02">
	<input type="checkbox" name="accordian" id="car02">
	<label for="car02">2023.07.17 | 09:00</label>
	<div><p><input type="date"></p></div>
</div>
<div  id="menu03">
	<input type="checkbox" name="accordian" id="car03">
	<label for="car03">전체</label>
	<div>
		<p><a href="#">전체</a></p><br>
		<p><a href="#">경형</a></p><br>
		<p><a href="#">소형</a></p><br>
		<p><a href="#">중형(세단)</a></p><br>
		<p><a href="#">중형(SUV)</a></p><br>
		<p><a href="#">전기차</a></p><br>
	</div>
</div>
<div  id="menu04">
	<input type="checkbox" name="accordian" id="car04">
	<label for="car04">대여시간</label>
	<div><p><input type="time">&nbsp;&nbsp;&nbsp;~&nbsp;&nbsp;&nbsp;<input type="time"></p></div>
</div>
</div>

<div class="clear"></div>

<div class="carname">
	<div><h1>KIA 레이</h1></div>
	<table border="1">
		<tr>
			<td>
				<img src="${path}/images/car/Gcar01.png">
			</td>
			<td id="sub">
				<p>차량명 : 2023 KIA 레이</p>
				<hr>
				<p>차량정보 : 가솔린 | 경형 RV | 2023년</p>
				<hr>
				<p>비고 :<br>나<br>는<br>레<br>이<br>임<br></p>
				<p><a href="${path}/rent/rent">차량 다시 선택</a></p>
			</td>
		</tr>
	</table>
	
	<div><h3>차량 예약하기</h3></div>
	<div id="carpay">
		<div id="box">
		
				<div id="member">
					<h2>&nbsp;예약자 정보</h2>
					아이디: <span>${rental.cr_mid}</span><br>
					연락처: <span>${mem.m_phone}</span><br>
					이름: <span>${mem.m_name}</span>
					<hr>

					<h2>&nbsp;차량 정보</h2>
					차종: <span>${car.c_name}</span><br>
					차량색상: <span>${car.c_color}</span><br>
					연식: <span>${car.c_year}년식</span><br>
				</div>

				<div id="car">
					<h2>&nbsp;렌트 정보</h2>
					예약일자: <span>${rental.cr_rdate}</span><br>
					실제 대여일자: <span>${rental.cr_sdate}부터</span><br>
					반납일자: <span>${rental.cr_edate}까지</span><br>
					렌트 비용: <span><fmt:formatNumber value="${total_price}" type="number" pattern="#,###"/>원</span><hr>
					
					
<button onclick="payMent('card', '${rental.cr_mid}', '${mem.m_phone}', '${mem.m_name}', '${car.c_name}', '${car.c_color}', '${car.c_year}', ${total_price})">카드 결제</button>
<button onclick="payMent('kakao', '${rental.cr_mid}', '${mem.m_phone}', '${mem.m_name}', '${car.c_name}', '${car.c_color}', '${car.c_year}', ${total_price})">카카오페이 결제</button>

					</div>
				</div>
			</div>
	<hr>
대여관련 안내사항
<br><br><br><br><br><br>
보험적용 불가사항
<br><br><br><br>
취소/환불안내
<br><br><br><br><br>
취소 및 환불규정
<br><br><br><br><br><br>
</div>



<div class="clear"></div>


<jsp:include page="../include/footer.jsp"/>
</body>
</html>