import java.util.ArrayList;
import java.util.List;

public class BookManager {

    // 🌟 核心武器：ArrayList (对标 Python 的 List)
    // 注意这里的 <Book>，它叫“泛型”。
    // 这体现了 Java 的严谨：它规定这个集合里【只能】装 Book 对象，如果谁敢往里塞个字符串或者数字，代码直接爆红报错！
    private List<Book> bookList = new ArrayList<>();

    // 1. 添加图书的方法
    public void addBook(Book book) {
        // bookList.add() 就完全等于 Python 里的 book_list.append()
        bookList.add(book);
        System.out.println("✅ 添加成功！《" + book.getBookName() + "》已入库。");
    }

    // 2. 查询所有图书的方法
    public void showAllBooks() {
        // 先判断仓库是不是空的 ( bookList.isEmpty() 等于 Python 的 len(book_list) == 0 )
        if (bookList.isEmpty()) {
            System.out.println("⚠️ 仓库当前空空如也，快去添加点书吧！");
            return; // 发现没书，直接结束当前方法，不往下走了
        }

        System.out.println("========== 当前馆藏图书 ==========");
        // 🌟 增强型 for 循环 (完美对标 Python 的 for b in book_list:)
        // 语法解析：从 bookList 里挨个拿出东西，赋值给左边的变量 b，b 的类型是 Book
        for (Book b : bookList) {
            // 这里你直接打印对象 b，Java 会极其聪明地去调用你刚才在 Book 类里写的 toString() 方法！
            System.out.println(b);
        }
        System.out.println("==================================");
    }
}