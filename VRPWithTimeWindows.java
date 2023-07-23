import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

class Solution {
    List<Vehicle> vehicles;

    public Solution(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public int getTotalDistance() {
        int totalDistance = 0;

        for (Vehicle vehicle : vehicles) {
            Location previousLocation = vehicle.depot;

            for (Customer customer : vehicle.route) {
                int distance = distance(previousLocation, customer.location);
                totalDistance += distance;
                previousLocation = customer.location;
            }

            int distanceToDepot = distance(previousLocation, vehicle.depot);
            totalDistance += distanceToDepot;
        }

        return totalDistance;
    }
    public int distance(Location location1, Location location2) {
        int dx = location1.x - location2.x;
        int dy = location1.y - location2.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
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
        Solution currentSolution = createInitialSolution();
        Solution bestSolution = currentSolution;
        List<Solution> tabuList = new ArrayList<>();

        int iteration = 0;
        int maxIterations = 10;
        int tabuSize = 10;

        while (iteration < maxIterations) {
            Solution neighborhoodSolution = generateNeighborhood(currentSolution);

            if (!tabuList.contains(neighborhoodSolution) && neighborhoodSolution.getTotalDistance() < currentSolution.getTotalDistance()) {
                currentSolution = neighborhoodSolution;

                if (currentSolution.getTotalDistance() < bestSolution.getTotalDistance()) {
                    bestSolution = currentSolution;
                }
            }

            tabuList.add(currentSolution);
            if (tabuList.size() > tabuSize) {
                tabuList.remove(0);
            }

            iteration++;
        }

        // Print the best solution
        printSolution(bestSolution);
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

    public Solution generateNeighborhood(Solution solution) {
        List<Vehicle> currentVehicles = solution.vehicles;
        int numVehicles = currentVehicles.size();

        // Select two random vehicles
        int vehicleIndex1 = (int) (Math.random() * numVehicles);
        int vehicleIndex2 = (int) (Math.random() * numVehicles);
        Vehicle vehicle1 = currentVehicles.get(vehicleIndex1);
        Vehicle vehicle2 = currentVehicles.get(vehicleIndex2);

        // Generate neighborhood solutions using different moves
        Solution bestNeighborhoodSolution = null;
        int bestObjectiveValue = Integer.MAX_VALUE;

        // Move a customer from vehicle1 to vehicle2
        for (int i = 0; i < vehicle1.route.size(); i++) {
            Customer customer = vehicle1.route.get(i);
            if (vehicle2.canAddCustomer(customer)) {
                vehicle1.route.remove(i);
                vehicle2.addCustomer(customer);

                int objectiveValue = solution.getTotalDistance();
                if (objectiveValue < bestObjectiveValue) {
                    bestObjectiveValue = objectiveValue;
                    bestNeighborhoodSolution = new Solution(new ArrayList<>(currentVehicles));
                }

                vehicle2.route.remove(customer);
                vehicle1.route.add(i, customer);
            }
        }

        // Swap two customers between vehicle1 and vehicle2
        for (int i = 0; i < vehicle1.route.size(); i++) {
            Customer customer1 = vehicle1.route.get(i);
            for (int j = 0; j < vehicle2.route.size(); j++) {
                Customer customer2 = vehicle2.route.get(j);
                if (vehicle1.canAddCustomer(customer2) && vehicle2.canAddCustomer(customer1)) {
                    vehicle1.route.set(i, customer2);
                    vehicle2.route.set(j, customer1);

                    int objectiveValue = solution.getTotalDistance();
                    if (objectiveValue < bestObjectiveValue) {
                        bestObjectiveValue = objectiveValue;
                        bestNeighborhoodSolution = new Solution(new ArrayList<>(currentVehicles));
                    }

                    vehicle1.route.set(i, customer1);
                    vehicle2.route.set(j, customer2);
                }
            }
        }

        // Reinsert a customer within vehicle
        // for (int i = 0; i < vehicle1.route.size(); i++) {
        //     Customer customer = vehicle1.route.remove(i);
        //     for (int j = 0; j <= vehicle1.route.size(); j++) {
        //         vehicle1.route.add(j, customer);

        //         int objectiveValue = solution.getObjectiveValue();
        //         if (objectiveValue < bestObjectiveValue) {
        //             bestObjectiveValue = objectiveValue;
        //             bestNeighborhoodSolution = new Solution(new ArrayList<>(currentVehicles));
        //         }

        //         vehicle1.route.remove(j);
        //     }
        //     vehicle1.route.add(i, customer);
        // }

        return bestNeighborhoodSolution;
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
                int waitingTime = Math.max(0, customer.earliest_arrival_time - currentTime);
                int serviceTime = customer.serving_time;
                int departureTime = arrivalTime + serviceTime;
                totalDemand += customer.demand;
                totalDistance += travelTime;
                totalServiceTime += serviceTime;
                int remainingCapacity = vehicle.capacity - totalDemand;


                System.out.println("From (" + previousLocation.x + ", " + previousLocation.y + ") to (" + currentLocation.x + ", " + currentLocation.y + ")");
                System.out.println("Travel Distance: " + travelTime);
                System.out.println("Arrival Time: " + arrivalTime);
                System.out.println("Waiting Time: " + waitingTime);
                System.out.println("Service Time: " + serviceTime);
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
            System.out.println("Total Service Time: " + totalServiceTime);
            System.out.println("////////////////////////////////////////////////////////////////");
            System.out.println();
        }
        // System.out.println("All Total Distance: " + allTotalDistance);
    }


    public static void main(String[] args) {
        // Create customers
        // String filePath = "E:/vrp/VRPTW_data_sample_1.xlsx";
        // try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
        //     Workbook workbook = WorkbookFactory.create(fileInputStream);
        //     Sheet sheet = workbook.getSheetAt(0); // Assuming the data is on the first sheet

        //     List<Customer> customers = new ArrayList<>();

        //     for (Row row : sheet) {
        //         if (row.getRowNum() == 0) {
        //             continue; // Skip the header row
        //         }

        //         int id = (int) row.getCell(0).getNumericCellValue();
        //         int demand = (int) row.getCell(3).getNumericCellValue();
        //         int earliest_arrival_time = (int) row.getCell(4).getNumericCellValue();
        //         int latest_arrival_time = (int) row.getCell(5).getNumericCellValue();
        //         int serving_time = (int) row.getCell(6).getNumericCellValue();
        //         int x = row.getCell(1).getNumericCellValue();
        //         int y = row.getCell(2).getNumericCellValue();
        //         boolean visited = false;

        //         Location location = new Location(x, y);
        //         Customer customer = new Customer(id, demand, earliest_arrival_time, latest_arrival_time, serving_time, location, visited);
        //         customers.add(customer);
        //     }

        //     // Print the customers
        //     for (Customer customer : customers) {
        //         System.out.println(customer);
        //     }
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

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
        // vrp.solve();
        Solution solutionx = vrp.createInitialSolution();
        vrp.printSolution(solutionx);
        System.out.println("Best Solution: " + solutionx.getTotalDistance());
    }


    private int distance(Location location1, Location location2) {
        int dx = location1.x - location2.x;
        int dy = location1.y - location2.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }
}



