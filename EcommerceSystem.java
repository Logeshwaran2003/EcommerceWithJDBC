import java.sql.*;
import java.util.*;

class Customer {
    private int cust_id;
    private String cust_name;
    private String username;
    private String password;

    public Customer(int cust_id, String cust_name, String username, String password) {
        this.cust_id = cust_id;
        this.cust_name = cust_name;
        this.username = username;
        this.password = password;
    }

    public int getCust_id() {
        return cust_id;
    }

    public String getCust_name() {
        return cust_name;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }
}

class Product {
    private int prod_id;
    private String prod_name;
    private int prod_price;

    public Product(int prod_id, String prod_name, int prod_price) {
        this.prod_id = prod_id;
        this.prod_name = prod_name;
        this.prod_price = prod_price;
    }

    public int getProd_id() {
        return prod_id;
    }

    public String getProd_name() {
        return prod_name;
    }

    public int getPrice() {
        return prod_price;
    }

    public void displayProductInfo() {
        System.out.println("Product_id: " + prod_id);
        System.out.println("Product_name: " + prod_name);
        System.out.println("Product_price: " + prod_price);
    }
}

class Order {
    private static int orderCounter = 1;

    private int order_id;
    private Customer customer;
    private List<Product> products;
    private int count;

    public Order(Customer customer, List<Product> products, int count) {
        this.order_id = orderCounter++;
        this.customer = customer;
        this.products = products;
        this.count = count;
    }

    public void displayOrderInfo() {
        System.out.println("Order ID: " + order_id);
        System.out.println("Customer: " + customer.getCust_name());
        System.out.println("Products:");
        for (Product product : products) {
            product.displayProductInfo();
        }
        System.out.println("Quantity: " + count);
    }
}

public class ECommerceSystem {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/ecommerce";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Logesh@123";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            createCustomersTable(connection);
            createProductsTable(connection);
            createOrdersTable(connection);

            // Insert sample customer and product details
            insertSampleData(connection);

            Scanner scan = new Scanner(System.in);

            // Login
            Customer loggedInCustomer = login(connection, scan);
            if (loggedInCustomer != null) {
                System.out.println("Login successful! Welcome, " + loggedInCustomer.getCust_name());

                // List products
                listProducts(connection);

                // Order products
                orderProducts(connection, scan, loggedInCustomer);

                // Display orders for the logged-in customer
                List<Order> orders = getOrdersForCustomer(connection, loggedInCustomer);
                if (!orders.isEmpty()) {
                    System.out.println("Orders for customer " + loggedInCustomer.getCust_name() + ":");
                    for (Order order : orders) {
                        order.displayOrderInfo();
                    }
                } else {
                    System.out.println("No orders found for customer " + loggedInCustomer.getCust_name());
                }
            } else {
                System.out.println("Login failed. Exiting...");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createCustomersTable(Connection connection) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS customers (" +
                "cust_id INT AUTO_INCREMENT PRIMARY KEY," +
                "cust_name VARCHAR(255) NOT NULL," +
                "username VARCHAR(255) NOT NULL," +
                "password VARCHAR(255) NOT NULL" +
                ")";
        executeUpdate(connection, createTableSQL, "Table 'customers' created successfully");
    }

    private static void createProductsTable(Connection connection) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS products (" +
                "prod_id INT AUTO_INCREMENT PRIMARY KEY," +
                "prod_name VARCHAR(255) NOT NULL," +
                "prod_price INT NOT NULL" +
                ")";
        executeUpdate(connection, createTableSQL, "Table 'products' created successfully");
    }

    private static void createOrdersTable(Connection connection) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INT AUTO_INCREMENT PRIMARY KEY," +
                "customer_id INT," +
                "product_id INT," +
                "quantity INT," +
                "FOREIGN KEY (customer_id) REFERENCES customers(cust_id)," +
                "FOREIGN KEY (product_id) REFERENCES products(prod_id)" +
                ")";
        executeUpdate(connection, createTableSQL, "Table 'orders' created successfully");
    }

    private static void executeUpdate(Connection connection, String sql, String successMessage) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            System.out.println(successMessage);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Customer login(Connection connection, Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String selectSQL = "SELECT * FROM customers WHERE username = ? AND password = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Customer(
                        resultSet.getInt("cust_id"),
                        resultSet.getString("cust_name"),
                        resultSet.getString("username"),
                        resultSet.getString("password")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void listProducts(Connection connection) {
        System.out.println("List of Products:");
        String selectSQL = "SELECT * FROM products";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectSQL)) {
            while (resultSet.next()) {
                int prod_id = resultSet.getInt("prod_id");
                String prod_name = resultSet.getString("prod_name");
                int prod_price = resultSet.getInt("prod_price");

                Product product = new Product(prod_id, prod_name, prod_price);
                product.displayProductInfo();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void orderProducts(Connection connection, Scanner scanner, Customer customer) {
        System.out.print("Enter the product ID to order: ");
        int productId = scanner.nextInt();
        System.out.print("Enter the quantity: ");
        int quantity = scanner.nextInt();

        String insertSQL = "INSERT INTO orders (customer_id, product_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setInt(1, customer.getCust_id());
            preparedStatement.setInt(2, productId);
            preparedStatement.setInt(3, quantity);
            preparedStatement.executeUpdate();
            System.out.println("Order placed successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<Order> getOrdersForCustomer(Connection connection, Customer customer) {
        List<Order> orders = new ArrayList<>();
        String selectSQL = "SELECT * FROM orders WHERE customer_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setInt(1, customer.getCust_id());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int orderId = resultSet.getInt("order_id");
                int productId = resultSet.getInt("product_id");
                int quantity = resultSet.getInt("quantity");

                // Retrieve product details
                Product product = getProductById(connection, productId);

                // Create an order
                Order order = new Order(customer, List.of(product), quantity);
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    private static Product getProductById(Connection connection, int productId) {
        String selectSQL = "SELECT * FROM products WHERE prod_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setInt(1, productId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Product(
                        resultSet.getInt("prod_id"),
                        resultSet.getString("prod_name"),
                        resultSet.getInt("prod_price")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void insertSampleData(Connection connection) {
        // Insert sample customers
        executeUpdate(connection, "INSERT INTO customers (cust_name, username, password) VALUES ('Logeshwaran', 'Logesh', 'Logesh@123')", "Sample customer inserted");
        executeUpdate(connection, "INSERT INTO customers (cust_name, username, password) VALUES ('Sasikumar', 'Sasi', 'Sasi@123')", "Sample customer inserted");

        // Insert sample products
        executeUpdate(connection, "INSERT INTO products (prod_name, prod_price) VALUES ('Mobile1', 12000)", "Sample product inserted");
        executeUpdate(connection, "INSERT INTO products (prod_name, prod_price) VALUES ('Mobile2', 10000)", "Sample product inserted");
    }
}
