package jang.worktogether.Utils;


public class ErrorcodeUtil {
    public static String errorMessage(String response){
        switch (response){
            case "101" :
                return "해당하는 이메일의 유저가 존재하지 않습니다";
            case "102" :
                return "해당 그룹은 존재하지 않습니다";
            case "103" :
                return "해당 채팅방은 존재하지 않습니다";
            case "104" :
                return "유저가 해당 그룹에 속하지 않습니다";
            case "105" :
                return "세션이 만료되었습니다";
            case "106" :
                return "이미 동일한 이메일이 가입되어 있습니다";
            case "107" :
                return "유저 정보에서 일치하는 이메일을 찾을 수 없습니다";
            case "108" :
                return "그룹장은 그룹에서 나갈 수 없습니다";
            case "109" :
                return "이미 탈퇴한 그룹입니다";
            case "110" :
                return "이미 존재하는 이메일입니다";
            case "111" :
                return "이미 로그인되어있는 아이디입니다";
            case "120" :
                return "가입하지 않은 이메일이거나 패스워드가 잘못되었습니다";
            case "140" :
                return "사진 업로드에 실패했습니다";
            case "170" :
                return "그룹 만들기에 실패했습니다";
            case "171" :
                return "그룹원을 초대하지 못했습니다";
            case "200" :
                return "DB와 연결할 수 없습니다";
            default:
                return "오류";
        }
    }
}
