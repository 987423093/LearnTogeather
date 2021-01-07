import java.util.Date;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2020/12/28 15:31
 * @Copyright: 2020 Hundsun All rights reserved.
 */
public class MainTest {

    public static void main(String[] args) {

        String str = "XinYuZang";
        Date date = new Date();
        MyBuilder myBuilder = MyBuilder.toBuilder()
                .str(str)
                .date(date)
                .build();
        System.out.println(myBuilder.getStr());
    }
}
