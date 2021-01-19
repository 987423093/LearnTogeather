import java.util.Date;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2020/12/28 15:25
 * @Copyright: 2020 Hundsun All rights reserved.
 */
public class MyBuilder {

    /**
     * 参数1
     */
    private String str;

    /**
     * 参数2
     */
    private Date date;

    /**
     * 构造函数，参数1+参数2
     * @param str
     */
    public MyBuilder(String str, Date date) {
        if (str == null) {
            System.out.println("str is not be null!");
        }
        if (date == null) {
            System.out.println("date is not be null!");
        }
        this.str = str;
        this.date = date;
    }

    /**
     * 参数1get方法
     * @return
     */
    public String getStr() {
        return str;
    }

    /**
     * 参数2get方法
     * @return
     */
    public Date getDate() {
        return date;
    }

    /**
     * 静态方法构造
     * @return
     */
    public static MyBuilder.Builder builder() {
        return new Builder();
    }

    public static MyBuilder.Builder toBuilder() {
        return new Builder();
    }

    /**
     * 静态内部类
     */
    public static class Builder {

        /**
         * 静态内部类——参数1
         */
        private String str;

        /**
         * 静态内部类-参数2
         */
        private Date date;

        /**
         * 静态内部类——普通方法设置参数1
         * @param str
         */
        public Builder str(String str) {
            this.str = str;
            return this;
        }

        /**
         * 静态内部类——普通方法设置参数2
         * @param date
         * @return
         */
        public Builder date(Date date) {
            this.date = date;
            return this;
        }

        /**
         * 静态内部类——build方法
         * @return
         */
        public MyBuilder build() {
            return new MyBuilder(this.str, this.date);
        }
    }
}
