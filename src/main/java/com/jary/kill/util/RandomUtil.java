package com.jary.kill.util;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {

    private static final SimpleDateFormat dateFormatOne=new SimpleDateFormat("yyyyMMddHHmmssSS");

    private static final ThreadLocalRandom random=ThreadLocalRandom.current();

    /**
     * 生成订单编号-方式一
     * @return
     */
    //TODO:时间戳+N位随机数作为订单编号
    public static String generateOrderCode(){
        return dateFormatOne.format(DateTime.now().toDate()) + generateNumber(4);
    }

    // num位随机数
    public static String generateNumber(final int num){
        StringBuffer sb=new StringBuffer();
        for (int i=1;i<=num;i++){
            sb.append(random.nextInt(9));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String salt="11299c42bf954c0abb373efbae3f6b26";
        String password="debug";
        System.out.println(new Md5Hash(password,salt));
    }
}