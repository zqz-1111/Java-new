// 实体类（Java Bean）：专门用来存数据的模版
public class Book {

    // 1. 属性私有化 (private)：极其重要！
    // 相当于给每个属性上了锁，外面的人不能直接用 book.price = -100 乱改数据
    private int id;
    private String bookName;
    private String author;
    private double price;

    // 2. 空参构造方法 (No-args Constructor)
    // 相当于 Python 里的 def __init__(self): 
    // Java 规矩：不管用不用，空参构造必须写上，很多框架（包括以后的 Spring）底层强制要求要有它！
    public Book() {
    }

    // 3. 全参构造方法 (All-args Constructor)
    // 让你在 new 对象的时候，可以直接把数据塞进去
    public Book(int id, String bookName, String author, double price) {
        // this 关键字，就完全等于 Python 里的 self
        this.id = id;
        this.bookName = bookName;
        this.author = author;
        this.price = price;
    }

    // 4. Getter 和 Setter 方法
    // 既然属性被 private 锁住了，就必须提供公开的(public)方法让别人读取(get)和修改(set)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        // 甚至可以在这里写逻辑：如果传进来的价格小于0，就不让修改！这就是封装的好处。
        this.price = price;
    }

    // 5. 重写 toString 方法 (可选，但极其好用)
    // 相当于 Python 里的 def __str__(self):
    // 以后你直接 System.out.println(book) 的时候，打印出来的就不是一串内存地址，而是漂亮的书籍信息
    @Override
    public String toString() {
        return "书籍编号: " + id + ", 书名: 《" + bookName + "》, 作者: " + author + ", 价格: ¥" + price;
    }
}