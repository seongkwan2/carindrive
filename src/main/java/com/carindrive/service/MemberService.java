package com.carindrive.service;

import java.util.List;

/*
import com.carindrive.vo.MemberVO;

public interface MemberService {

	//회원가입 메서드
	//void insertMember(MemberVO m);

	//회원정보를 가져오는 메서드(로그인)
	//MemberVO getMemberInfo(String m_id);


}*/

import javax.transaction.Transactional;


import com.carindrive.vo.MemberVO;
import com.carindrive.vo.SocialVO;


public interface MemberService {
	
	
	//회원가입 메서드
	//void insertMember(MemberVO m);

	//회원정보를 가져오는 메서드(로그인)
	MemberVO getMemberInfo(String m_id);

	MemberVO idCheck(String id);

	void insertMember(MemberVO cm);

	MemberVO loginCheck(String m_id);

	MemberVO adminCk(String m_id);

	void insertKakao(SocialVO kakaoUser);

	SocialVO serchkakao(String userEmail);
	
	MemberVO serchUserEmail(String m_email);

	int updatePwd(MemberVO m);

	List<MemberVO> myPage(String m_id);

	MemberVO findPwd(String m_id);

	void updateMember(MemberVO m);

	void delMember(String m_id);



	

}

	
