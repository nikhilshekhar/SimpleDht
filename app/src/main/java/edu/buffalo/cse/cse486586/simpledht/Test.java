package edu.buffalo.cse.cse486586.simpledht;

/**
 * Created by nikhilshekhar on 4/6/16.
 */
public class Test {
    public static void main(String args[]){
//        String s54 ="33d6357cfaaf0f72991b0ecd8c56da066613c089";
//        String s56 ="208f7f72b198dadd244e61801abe1ec3a4857bc9";
//        String s58 ="abf0fd8db03e5ecb199a9b82929e9db79b909643";
//        String s60 ="c25ddd596aa7c81fa12378fa725f706d54325d12";
//        String s62 ="177ccecaec32c54b82d5aaafc18a2dadb753e3b1";
//
//        System.out.println(s54.compareTo(s56));//s56<s54
//        System.out.println(s54.compareTo(s58));//s58>s54
//        System.out.println(s54.compareTo(s60));//s60>s54
//        System.out.println(s54.compareTo(s62));//s62<s54
//        System.out.println(s58.compareTo(s60));//s60>s58
//        System.out.println(s56.compareTo(s62));//s62<s56
        String d = "\\$";

        String s = "FlDTedDjRSls4w939b7AAKWyDsrzMG4s$1dB7PEqw3eKeqStUDGOAzSgxf815P6sG";
        System.out.println(s.split(d)[0]);
        System.out.println(s.split(d)[1]);

    }
}
