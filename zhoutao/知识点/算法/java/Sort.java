import java.util.Arrays;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/1/18 15:45
 */
public class Sort {

    public static void main(String[] args) {

        int[] array = new int[]{3, 4, 5, 1, 6, 7, 2};
        Sort sort = new Sort();

        // 快排
        sort.quickSort(array);
        System.out.println(Arrays.toString(array));

    }

    /**
     * 快速排序
     *
     * @param array
     * @desc 1. 找基准 2.将基准的左右不断替换
     */
    private void quickSort(int[] array) {

        quickSort(array, 0, array.length - 1);
    }

    private void quickSort(int[] array, int low, int high) {

        if (low >= high || array.length <= 1) {
            return;
        }
        // 左边
        int i = low;
        // 右边
        int j = high;
        // 基准
        int index = array[i];
        while (i < j) {
            // 从右往左找小于标准的值，放到左边去
            while (i < j && array[j] >= index) {
                j--;
            }
            if (i < j) {
                array[i] = array[j];
                i++;
            }
            // 从左往右找大于标准的值，放到右边去
            while (i < j && array[i] <= index) {
                i++;
            }
            if (i < j) {
                array[j] = array[i];
                j--;
            }
        }
        array[i] = index;
        quickSort(array, low, i - 1);
        quickSort(array, i + 1, high);
    }



}
