import java.util.Stack;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/2/22 15:18
 * @Copyright: 2020 Hundsun All rights reserved.
 */
public class 有效的括号20 {

    public static boolean isValid(String s) {

        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < s.length(); i++) {
            if (stack.isEmpty()) {
                stack.push(s.charAt(i));
            } else {
                Character peek = stack.peek();
                if (s.charAt(i) == ')' && peek == '(') {
                    stack.pop();
                } else if (s.charAt(i) == ']' && peek == '[') {
                    stack.pop();
                } else if (s.charAt(i) == '}' && peek == '{') {
                    stack.pop();
                } else {
                    stack.push(s.charAt(i));
                }
            }
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {

        String s = "([{]}){}";
        System.out.println(isValid(s));
    }
}
