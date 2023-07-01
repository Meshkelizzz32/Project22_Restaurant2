package project28;

import java.util.concurrent.*;
import java.util.*;


class OrderTicket {
	 private static int counter;
	 private final int id = counter++;
	 private final Table table;
	 private final List<Order> orders =
	 Collections.synchronizedList(new LinkedList<Order>());
	 public OrderTicket(Table table) { this.table = table; }
	 public WaitPerson getWaitPerson() {
	 return table.getWaitPerson();
	 }
	 public void placeOrder(Customer cust, Food food) {
	 Order order = new Order(cust, food);
	 orders.add(order);
	 order.setOrderTicket(this);

	 }
	 public List<Order> getOrders() { return orders; }
	 public String toString() {
	 StringBuilder sb = new StringBuilder(
	 "Order Ticket: " + id + " for: " + table + "\n");
	 synchronized(orders) {
	 for(Order order : orders)
	 sb.append(order.toString() + "\n");
	 }
	 return sb.substring(0, sb.length() - 1).toString();
	 }
	}
	class Table implements Runnable {
	 private static int counter;
	 private final int id = counter++;
	 private final WaitPerson waitPerson;
	 private final List<Customer> customers;
	 private final OrderTicket orderTicket =
	 new OrderTicket(this);
	 private final CyclicBarrier barrier;
	 private final int nCustomers;
	 private final ExecutorService exec;
	 public Table(WaitPerson waitPerson, int nCustomers,
	 ExecutorService e) {
	 this.waitPerson = waitPerson;
	 customers = Collections.synchronizedList(
	 new LinkedList<Customer>());
	 this.nCustomers = nCustomers;
	 exec = e;
	 barrier = new CyclicBarrier(nCustomers + 1,
	 new Runnable() {
	 public void run() {
		 System.out.println(orderTicket.toString());
	 }
	 });
	 }
	 public WaitPerson getWaitPerson() { return waitPerson; }
	 public void placeOrder(Customer cust, Food food) {
	 orderTicket.placeOrder(cust, food);
	 }
	 public void run() {
	 Customer customer;
	 for(int i = 0; i < nCustomers; i++) {
	 customers.add(customer = new Customer(this, barrier));
	 exec.execute(customer);

	 }
	 try {
	 barrier.await();
	 } catch(InterruptedException ie) {
		 System.out.println(this + " interrupted");
	 return;
	 } catch(BrokenBarrierException e) {
	 throw new RuntimeException(e);
	 }
	 waitPerson.placeOrderTicket(orderTicket);
	 }
	 public String toString() {
	 StringBuilder sb = new StringBuilder(
	 "Table: " + id + " served by: " + waitPerson + "\n");
	 synchronized(customers) {
	 for(Customer customer : customers)
	 sb.append(customer.toString() + "\n");
	 }
	 return sb.substring(0, sb.length() - 1).toString();
	 }
	}
	
	class Order {
	 private static int counter;
	 private final int id;
	 private volatile OrderTicket orderTicket;
	 private final Customer customer;
	 private final Food food;
	 public Order(Customer cust, Food f) {
	 customer = cust;
	 food = f;
	 synchronized(Order.class) { id = counter++; }
	 }
	 void setOrderTicket(OrderTicket orderTicket) {
	 this.orderTicket = orderTicket;
	 }
	 public OrderTicket getOrderTicket() {
	 return orderTicket;
	 }
	 public Food item() { return food; }
	 public Customer getCustomer() { return customer; }
	 public String toString() {
	 return "Order: " + id + " item: " + food +
	 " for: " + customer;
	 }
	}
	class Plate {
	 private final Order order;
	 private final Food food;
	 public Plate(Order ord, Food f) {
	 order = ord;
	 food = f;
	 }
	 public Order getOrder() { return order; }
	 public Food getFood() { return food; }
	 public String toString() { return food.toString(); }
	}
	class Customer implements Runnable {
	 private static int counter;
	 private final int id;
	 private final CyclicBarrier barrier;
	 private final Table table;
	 public Customer(Table table, CyclicBarrier barrier) {
	 this.table = table;
	 this.barrier = barrier;
	 synchronized(Customer.class) { id = counter++; }
	 }
	 private final SynchronousQueue<Plate> placeSetting =
	 new SynchronousQueue<Plate>();
	 public void
	 deliver(Plate p) throws InterruptedException {
	 placeSetting.put(p);
	 }
	 public void run() {
	 for(Course course : Course.values()) {
	 Food food = course.randomSelection();
	 table.placeOrder(this, food);
	 ++counter;
	 }
	 try {
	 barrier.await();
	 } catch(InterruptedException ie) {
		 System.out.println(this + " interrupted while ordering meal");
	 return;
	 } catch(BrokenBarrierException e) {
	 throw new RuntimeException(e);
	 }
	 for(int i = 0; i < counter; i++)
	 try {
		 System.out.println(this + "eating " + placeSetting.take());
	 } catch(InterruptedException e) {
		 System.out.println(this + "waiting for meal interrupted");
	 return;
	 }
	 System.out.println(this + "finished meal, leaving");
	 }
	 public String toString() {
	 return "Customer " + id + " ";
	 }
	}
	class WaitPerson implements Runnable {
	 private static int counter;
	 private final int id = counter++;
	 private final Restaurant restaurant;
	 final BlockingQueue<Plate> filledOrders =
	 new LinkedBlockingQueue<Plate>();
	 public WaitPerson(Restaurant rest) { restaurant = rest; }
	 public void placeOrderTicket(OrderTicket orderTicket) {
	 try {
	 restaurant.orderTickets.put(orderTicket);
	 } catch(InterruptedException e) {
		 System.out.println(this + " placeOrderTicket interrupted");
	 }
	 }
	 public void run() {
	 try {
	 while(!Thread.interrupted()) {
	 Plate plate = filledOrders.take();
	 System.out.println(this + "received " + plate +
	 " delivering to " +
	 plate.getOrder().getCustomer());
	 plate.getOrder().getCustomer().deliver(plate);
	 }
	 } catch(InterruptedException e) {
		 System.out.println(this + " interrupted");
	 }
	 System.out.println(this + " off duty");
	 }
	 public String toString() {
	 return "WaitPerson " + id + " ";
	 }
	}
	class Chef implements Runnable {
	 private static int counter;
	 private final int id = counter++;
	 private final Restaurant restaurant;
	 private static Random rand = new Random(47);
	 public Chef(Restaurant rest) { restaurant = rest; }
	 public void run() {
	 try {
	 while(!Thread.interrupted()) {
	 OrderTicket orderTicket =
	 restaurant.orderTickets.take();
	 List<Order> orders = orderTicket.getOrders();
	 synchronized(orders) {
	 for(Order order : orders) {
	 Food requestedItem = order.item();
	 TimeUnit.MILLISECONDS.sleep(rand.nextInt(500));
	 Plate plate = new Plate(order, requestedItem);
	 order.getOrderTicket().getWaitPerson().
	 filledOrders.put(plate);
	 }
	 }
	 }
	 } catch(InterruptedException e) {
		 System.out.println(this + " interrupted");
	 }
	 System.out.println(this + " off duty");
	 }
	 public String toString() { return "Chef " + id + " "; }
	}
	class Restaurant implements Runnable {
	 private List<WaitPerson> waitPersons =
	 new ArrayList<WaitPerson>();
	 private List<Chef> chefs = new ArrayList<Chef>();
	 private ExecutorService exec;
	 private static Random rand = new Random(47);
	 final BlockingQueue<OrderTicket> orderTickets =
	 new LinkedBlockingQueue<OrderTicket>();
	 public Restaurant(ExecutorService e, int nWaitPersons,
	 int nChefs) {
	 exec = e;
	 for(int i = 0; i < nWaitPersons; i++) {
	 WaitPerson waitPerson = new WaitPerson(this);
	 waitPersons.add(waitPerson);
	 exec.execute(waitPerson);
	 }
	 for(int i = 0; i < nChefs; i++) {
	 Chef chef = new Chef(this);
	 chefs.add(chef);
	 exec.execute(chef);
	 }
	 }
	 public void run() {
	 try {
	 while(!Thread.interrupted()) {
	 // A new group of customers arrive; assign a
	 // WaitPerson:
	 WaitPerson wp = waitPersons.get(
	 rand.nextInt(waitPersons.size()));
	 int nCustomers = rand.nextInt(4) + 1;
	 Table t = new Table(wp, nCustomers, exec);
	 exec.execute(t);
	 TimeUnit.MILLISECONDS.sleep(400 * nCustomers);
	 }
	 } catch(InterruptedException e) {
		 System.out.println("Restaurant interrupted");
	 }
	 System.out.println("Restaurant closing");
	 }
	}
	public class RestaurantWithQueues {
	 public static void main(String[] args) throws Exception {
	 ExecutorService exec = Executors.newCachedThreadPool();
	 Restaurant restaurant = new Restaurant(exec, 5, 2);
	 exec.execute(restaurant);
	 if(args.length > 0) // Optional argument
	 TimeUnit.SECONDS.sleep(new Integer(args[0]));
	 else {
	 System.out.println("Press 'ENTER' to quit");
	 System.in.read();
	 }
	 exec.shutdownNow();
	 }
	}