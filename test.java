import java.util.ArrayList;
import java.util.List;


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
        int maxIterations = 1000;
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
        // Apply greedy algorithm to create an initial solution
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
    
                        if (arrivalTime <= customer.latest_arrival_time && arrivalTime >= customer.earliest_arrival_time
                                && arrivalTime < bestArrivalTime) {
                            bestCustomer = customer;
                            bestVehicle = vehicle;
                            bestArrivalTime = arrivalTime;
                        }
                    }
                }
            }
    
            if (bestCustomer != null && bestVehicle != null) {
                bestVehicle.addCustomer(bestCustomer);
                bestCustomer.isVisited = true;
                unvisitedCustomers.remove(bestCustomer);
            } else {
                System.out.println("No feasible solution for remaining customers.");
                break;
            }
        }
    
        return initialSolution;
    }

    private List<Solution> generateNeighborhood(Solution solution) {
        List<Solution> neighborhood = new ArrayList<>();
        List<Vehicle> vehicles = solution.vehicles;
        int numVehicles = vehicles.size();


        for (int i = 0; i < numVehicles; i++) {
            Vehicle vehicle1 = vehicles.get(i);
            List<Customer> route1 = vehicle1.route;
            int numCustomers1 = route1.size();


            for (int j = 0; j < numCustomers1; j++) {
                Customer customer1 = route1.get(j);
                Vehicle vehicle2 = vehicles.get((i + 1) % numVehicles);
                List<Customer> route2 = vehicle2.route;
                int numCustomers2 = route2.size();


                for (int k = 0; k < numCustomers2; k++) {
                    Customer customer2 = route2.get(k);


                    if (vehicle1.canAddCustomer(customer2) && vehicle2.canAddCustomer(customer1)) {
                        // Create a new solution by swapping customers between two vehicles
                        Solution newSolution = solution.clone();
                        route1.remove(j);
                        route2.add(k, customer1);
                        newSolution.updateTotalDistance();


                        neighborhood.add(newSolution);


                        // Restore the original routes for the next iteration
                        route1.add(j, customer1);
                        route2.remove(k);
                    }
                }
            }
        }


        return neighborhood;
    }


    private Solution findBestNeighbor(List<Solution> neighborhood, List<Solution> tabuList) {
        Solution bestNeighbor = null;
        double bestDistance = Double.POSITIVE_INFINITY;


        for (Solution neighbor : neighborhood) {
            if (!tabuList.contains(neighbor) && neighbor.totalDistance < bestDistance) {
                bestNeighbor = neighbor;
                bestDistance = neighbor.totalDistance;
            }
        }


        return bestNeighbor;
    }


    private void printSolution(Solution solution) {
        double allTotalDistance = 0;


        for (Vehicle vehicle : solution.vehicles) {
            if (vehicle.route.isEmpty()) {
                System.out.println("All Total Distance: " + allTotalDistance);
                return;
            }
            Location previousLocation = vehicle.depot;
            int currentTime = 0;
            int index = solution.vehicles.indexOf(vehicle) + 1;
            System.out.println("Vehicle " + index + " route:");
            System.out.println("Depot: (" + vehicle.depot.x + ", " + vehicle.depot.y + ")");
            double totalDistance = 0;
            int totalServiceTime = 0;
            int totalDemand = 0;


            for (Customer customer : vehicle.route) {
                Location currentLocation = customer.location;
                int travelTime = distance(previousLocation, currentLocation);
                int arrivalTime = Math.max(currentTime + travelTime, customer.earliest_arrival_time);
                int serviceTime = customer.serving_time;
                int departureTime = arrivalTime + serviceTime;
                totalDemand += customer.demand;
                totalDistance += travelTime;
                totalServiceTime += serviceTime;
                int remainingCapacity = vehicle.capacity - totalDemand;


                System.out.println("From (" + previousLocation.x + ", " + previousLocation.y + ") to (" + currentLocation.x + ", " + currentLocation.y + ")");
                System.out.println("Travel Distance: " + travelTime);
                System.out.println("Arrival Time: " + arrivalTime);
                System.out.println("Departure Time: " + departureTime);
                System.out.println("Remaining Capacity: " + remainingCapacity);
                previousLocation = currentLocation;
                currentTime = departureTime;
            }


            // Add distance from the last customer to the depot
            double lastDistance = distance(previousLocation, vehicle.depot);
            totalDistance += lastDistance;
            allTotalDistance += totalDistance;


            System.out.println("From (" + previousLocation.x + ", " + previousLocation.y + ") to depot");
            System.out.println("Travel Distance: " + lastDistance);
            System.out.println("Total Distance Traveled: " + totalDistance);
            System.out.println("////////////////////////////////////////////////////////////////");
            System.out.println();
        }
        // System.out.println("All Total Distance: " + allTotalDistance);
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
        vrp.solve();
        // Solution solutionx = vrp.createInitialSolution();
        // vrp.printSolution(solutionx);
    }


    private int distance(Location location1, Location location2) {
        int dx = location1.x - location2.x;
        int dy = location1.y - location2.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }


    private class Solution implements Cloneable {
        List<Vehicle> vehicles;
        double totalDistance;


        public Solution(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
            this.totalDistance = 0;
            updateTotalDistance();
        }


        public Solution clone() {
            List<Vehicle> clonedVehicles = new ArrayList<>();
            for (Vehicle vehicle : vehicles) {
                Vehicle clonedVehicle = new Vehicle(vehicle.capacity, vehicle.depot);
                for (Customer customer : vehicle.route) {
                    clonedVehicle.addCustomer(customer);
                }
                clonedVehicles.add(clonedVehicle);
            }
            return new Solution(clonedVehicles);
        }


        public void updateTotalDistance() {
            totalDistance = vehicles.stream()
                    .mapToDouble(this::calculateRouteDistance)
                    .sum();
        }


        private double calculateRouteDistance(Vehicle vehicle) {
            List<Customer> route = vehicle.route;
            double distance = 0;
            Location previousLocation = vehicle.depot;


            for (Customer customer : route) {
                Location currentLocation = customer.location;
                distance += distance(previousLocation, currentLocation);
                previousLocation = currentLocation;
            }


            distance += distance(previousLocation, vehicle.depot);
            return distance;
        }
    }
}



