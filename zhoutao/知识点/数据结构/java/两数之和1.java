import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/2/22 15:10
 * @Copyright: 2020 Hundsun All rights reserved.
 */
public class 两数之和1 {

    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer value = map.get(target - nums[i]);
            map.put(nums[i], i);
            if (value != null && value != i) {
                return new int[] {i, value};
            }
        }
        return null;
    }
}
