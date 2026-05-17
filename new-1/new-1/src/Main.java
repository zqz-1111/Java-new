import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 1. 准备工具
        // Scanner 对标 Python 的 input()，但它更强大，需要先“实例化”
        Scanner sc = new Scanner(System.in);
        // 实例化我们刚才写的仓库管理器
        BookManager manager = new BookManager();

        System.out.println("✨ 欢迎使用 Java 纯手工图书管理系统 ✨");

        // 2. 开启死循环：让程序一直运行，直到用户选择退出
        while (true) {
            System.out.println("\n--- 功能菜单 ---");
            System.out.println("1. 添加图书");
            System.out.println("2. 查看所有图书");
            System.out.println("3. 退出系统");
            System.out.print("请输入指令序号：");

            // 接收键盘输入的一个整数
            int choice = sc.nextInt();

            // 3. 分支处理：根据用户的选择执行不同动作
            switch (choice) {
                case 1:
                    // 添加逻辑
                    System.out.print("请输入书名：");
                    String name = sc.next();
                    System.out.print("请输入作者：");
                    String author = sc.next();
                    System.out.print("请输入价格：");
                    double price = sc.nextDouble();

                    // 这里我们为了简单，ID暂时用书名哈希值或者随便给个数字，
                    // 以后连了数据库就会自动生成了。这里演示直接传个随机ID。
                    int id = (int)(Math.random() * 1000);

                    // 核心动作：捏出泥人 -> 扔进仓库
                    Book newBook = new Book(id, name, author, price);
                    manager.addBook(newBook);
                    break;

                case 2:
                    // 查询逻辑：直接调用仓库的方法
                    manager.showAllBooks();
                    break;

                case 3:
                    System.out.println("👋 感谢使用，再见！");
                    // 强制结束虚拟机运行
                    System.exit(0);
                    break;

                default:
                    System.out.println("❌ 输入有误，请重新输入！");
            }
        }
    }
}