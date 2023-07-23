import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class Location {
    int x;
    int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }
}


class Customer {
    int id;
    int demand;
    int earliest_arrival_time;
    int latest_arrival_time;
    int serving_time;
    Location location;
    boolean isVisited;

    public Customer(int id, int demand, int earliest_arrival_time, int latest_arrival_time, int serving_time, Location location , boolean isVisited) {
        this.id = id;
        this.demand = demand;
        this.earliest_arrival_time = earliest_arrival_time;
        this.latest_arrival_time = latest_arrival_time;
        this.serving_time = serving_time;
        this.location = location;
        this.isVisited = isVisited;
    }
}


class Vehicle {
    int capacity;
    Location depot;
    List<Customer> route;


    public Vehicle(int capacity, Location depot) {
        this.capacity = capacity;
        this.depot = depot;
        this.route = new ArrayList<>();
    }


    public boolean canAddCustomer(Customer customer) {
        int totalDemand = route.stream().mapToInt(c -> c.demand).sum();
        return totalDemand + customer.demand <= capacity;
    }


    public void addCustomer(Customer customer) {
        route.add(customer);
    }
}


public class VRPWithTimeWindows {
    private List<Customer> customers;
    private List<Vehicle> vehicles;


    public VRPWithTimeWindows(List<Customer> customers, List<Vehicle> vehicles) {
        this.customers = customers;
        this.vehicles = vehicles;
    }
    private int calculateArrivalTime(Vehicle vehicle, Customer customer) {
        int travelTime = 0;
        if (!vehicle.route.isEmpty()) {
            Customer lastCustomer = vehicle.route.get(vehicle.route.size() - 1);
            travelTime = lastCustomer.latest_arrival_time + distance(lastCustomer.location, customer.location);
        } else {
            travelTime = distance(vehicle.depot, customer.location);
        }
        return Math.max(customer.earliest_arrival_time, travelTime);
    }
    public void solve() {
        // Create initial solution using the greedy algorithm
        Solution currentSolution = createInitialSolution();
        // Initialize Tabu Search parameters
        int maxIterations = 100;
        int tabuListSize = 25;
        List<Solution> tabuList = new ArrayList<>();


        // Perform Tabu Search iterations
        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            List<Solution> neighborhood = generateNeighborhood(currentSolution);
            Solution bestNeighbor = findBestNeighbor(neighborhood, tabuList);
            tabuList.add(bestNeighbor);
            if (tabuList.size() > tabuListSize) {
                tabuList.remove(0);
            }
            currentSolution = bestNeighbor;
        }


        // Print the final solution
        printSolution(currentSolution);
    }


    private Solution createInitialSolution() {
        Solution initialSolution = new Solution(vehicles);
        List<Customer> unvisitedCustomers = new ArrayList<>(customers);

        while (!unvisitedCustomers.isEmpty()) {
            Customer bestCustomer = null;
            Vehicle bestVehicle = null;
            int bestArrivalTime = Integer.MAX_VALUE;

            for (Customer customer : unvisitedCustomers) {
                for (Vehicle vehicle : vehicles) {
                    if (vehicle.canAddCustomer(customer)) {
                        int arrivalTime = calculateArrivalTime(vehicle, customer);

                        if (arrivalTime <= customer.latest_arrival_time && arrivalTime < bestArrivalTime) {
                            bestCustomer = customer;
                            bestVehicle = vehicle;
                            bestArrivalTime = arrivalTime;
                        }
                    }
                }
            }

            if (bestCustomer != null && bestVehicle != null) {
                bestVehicle.addCustomer(bestCustomer);
                unvisitedCustomers.remove(bestCustomer);
            } else {
                break;
            }
        }

        return initialSolution;
    }

    private List<Solution> generateNeighborhood(Solution solution) {
        List<Solution> neighborhood = new ArrayList<>();

        for (int i = 0; i < solution.vehicles.size(); i++) {
            for (int j = 0; j < solution.vehicles.get(i).route.size(); j++) {
                for (int k = 0; k < solution.vehicles.size(); k++) {
                    if (i != k) {
                        Solution neighbor = new Solution(solution);
                        Customer customerToMove = neighbor.vehicles.get(i).route.get(j);
                        if (neighbor.vehicles.get(k).canAddCustomer(customerToMove)) {
                            neighbor.vehicles.get(i).route.remove(j);
                            neighbor.vehicles.get(k).addCustomer(customerToMove);
                            neighborhood.add(neighbor);
                        }
                    }
                }
            }
        }

        return neighborhood;
    }


    private Solution findBestNeighbor(List<Solution> neighborhood, List<Solution> tabuList) {
        Solution bestNeighbor = null;
        int bestCost = Integer.MAX_VALUE;

        for (Solution neighbor : neighborhood) {
            int cost = neighbor.calculateCost();
            if (cost < bestCost && !tabuList.contains(neighbor)) {
                bestNeighbor = neighbor;
                bestCost = cost;
            }
        }

        return bestNeighbor;
    }


    private void printSolution(Solution solution) {
        double allTotalDistance = 0;


        for (Vehicle vehicle : solution.vehicles) {
            Location previousLocation = vehicle.depot;
            int currentTime = 0;
            int index = solution.vehicles.indexOf(vehicle) + 1;
            // System.out.println("Vehicle " + index + " route:");
            // System.out.println("Depot: (" + vehicle.depot.x + ", " + vehicle.depot.y + ")");
            double totalDistance = 0;
            int totalServiceTime = 0;
            int totalDemand = 0;


            for (Customer customer : vehicle.route) {
                Location currentLocation = customer.location;
                int travelTime = distance(previousLocation, currentLocation);
                int arrivalTime = Math.max(currentTime + travelTime, customer.earliest_arrival_time);
                int waitingTime = Math.max(0, customer.earliest_arrival_time - currentTime);
                int serviceTime = customer.latest_arrival_time - arrivalTime;
                int departureTime = arrivalTime + Math.max(waitingTime, serviceTime);
                totalDemand += customer.demand;
                totalDistance += travelTime;
                totalServiceTime += serviceTime;
                int remainingCapacity = vehicle.capacity - totalDemand;


                // System.out.println("From (" + previousLocation.x + ", " + previousLocation.y + ") to (" + currentLocation.x + ", " + currentLocation.y + ")");
                // System.out.println("Travel Distance: " + travelTime);
                // System.out.println("Arrival Time: " + arrivalTime);
                // System.out.println("Waiting Time: " + waitingTime);
                // System.out.println("Service Time: " + serviceTime);
                // System.out.println("Departure Time: " + departureTime);
                // System.out.println("Remaining Capacity: " + remainingCapacity);
                previousLocation = currentLocation;
                currentTime = departureTime;
            }


            // Add distance from the last customer to the depot
            double lastDistance = distance(previousLocation, vehicle.depot);
            totalDistance += lastDistance;
            allTotalDistance += totalDistance;


            // System.out.println("From (" + previousLocation.x + ", " + previousLocation.y + ") to depot");
            // System.out.println("Travel Distance: " + lastDistance);
            // System.out.println("Total Distance Traveled: " + totalDistance);
            // System.out.println("Total Service Time: " + totalServiceTime);
            // System.out.println("////////////////////////////////////////////////////////////////");
            // System.out.println();
        }
        System.out.println("All Total Distance: " + allTotalDistance);
    }


    public static void main(String[] args) {
        // Create customers
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1, 0, 0, 230, 0, new Location(35, 35),false));
        customers.add(new Customer(2, 10, 161, 171, 10, new Location(41, 49),false));
        customers.add(new Customer(3, 7, 50, 60, 10, new Location(35, 17),false));
        customers.add(new Customer(4, 13, 116, 126, 10, new Location(55, 45),false));
        customers.add(new Customer(5, 19, 149, 159, 10, new Location(55, 20),false));
        customers.add(new Customer(6, 26, 34, 44, 10, new Location(15, 30),false));
        customers.add(new Customer(7, 3, 99, 109, 10, new Location(25, 30),false));
        customers.add(new Customer(8, 5, 81, 91, 10, new Location(20, 50),false));
        customers.add(new Customer(9, 9, 95, 105, 10, new Location(10, 43),false));
        customers.add(new Customer(10, 16, 97, 107, 10, new Location(55, 60),false));
        customers.add(new Customer(11, 16, 124, 134, 10, new Location(30, 60),false));
        customers.add(new Customer(12, 12, 67, 77, 10, new Location(20, 65),false));
        customers.add(new Customer(13, 19, 63, 73, 10, new Location(50, 35),false));
        customers.add(new Customer(14, 23, 159, 169, 10, new Location(30, 25),false));
        customers.add(new Customer(15, 20, 32, 42, 10, new Location(15, 10),false));
        customers.add(new Customer(16, 8, 61, 71, 10, new Location(30, 5),false));
        customers.add(new Customer(17, 19, 75, 85, 10, new Location(10, 20),false));
        customers.add(new Customer(18, 2, 157, 167, 10, new Location(5, 0),false));
        customers.add(new Customer(19, 12, 87, 97, 10, new Location(20, 40),false));
        customers.add(new Customer(20, 17, 76, 86, 10, new Location(15, 60),false));
        customers.add(new Customer(21, 9, 126, 136, 10, new Location(45, 65),false));
        customers.add(new Customer(22, 11, 62, 72, 10, new Location(45, 20),false));
        customers.add(new Customer(23, 18, 97, 107, 10, new Location(45, 10),false));
        customers.add(new Customer(24, 15, 60, 70, 10, new Location(45, 5),false));
        customers.add(new Customer(25, 4, 22, 32, 10, new Location(55, 5),false));


        // Create vehicles
        List<Vehicle> vehicles = new ArrayList<>();
            Location depot = new Location(0, 0);
            for (int i = 1; i <= 25; i++) {
                vehicles.add(new Vehicle(200, depot));
            }


        // Create VRPTW instance and solve
        VRPWithTimeWindows vrp = new VRPWithTimeWindows(customers, vehicles);
        // Solution solutionx = vrp.createInitialSolution();
        // vrp.printSolution(solutionx);
        vrp.solve();
        // Solution solutionx = vrp.createInitialSolution();
        // vrp.printSolution(solutionx);
    }


    private int distance(Location location1, Location location2) {
        int dx = location1.x - location2.x;
        int dy = location1.y - location2.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private class Solution {
        List<Vehicle> vehicles;

        public Solution(List<Vehicle> vehicles) {
            this.vehicles = new ArrayList<>(vehicles);
        }

        public Solution(Solution solution) {
            this.vehicles = new ArrayList<>();
            for (Vehicle vehicle : solution.vehicles) {
                Vehicle newVehicle = new Vehicle(vehicle.capacity, vehicle.depot);
                newVehicle.route.addAll(vehicle.route);
                this.vehicles.add(newVehicle);
            }
        }

        public int calculateCost() {
        int cost = 0;
        for (Vehicle vehicle : vehicles) {
            cost += vehicle.route.stream()
                    .mapToInt(c -> distance(c.location, vehicle.depot))
                    .sum();
        }
        return cost;
    }
    }
}



