public class Demo {
    public static void main(String[] args) {
        System.out.println("Hello from JTextEditor!");
        System.out.println("----------------------------");

        // A little loop
        System.out.println("Counting to 5:");
        for (int i = 1; i <= 5; i++) {
            System.out.println("  " + i);
        }

        // A little math
        int n = 10;
        System.out.println(n + "! = " + factorial(n));

        // FizzBuzz, because of course
        System.out.println("FizzBuzz 1..15:");
        for (int i = 1; i <= 15; i++) {
            if (i % 15 == 0) {
                System.out.println("  FizzBuzz");
            } else if (i % 3 == 0) {
                System.out.println("  Fizz");
            } else if (i % 5 == 0) {
                System.out.println("  Buzz");
            } else {
                System.out.println("  " + i);
            }
        }

        System.out.println("----------------------------");
        System.out.println("Done. Edit me and press Cmd+R to run again!");
    }

    private static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
