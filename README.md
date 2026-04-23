# Order A Taxi (OrderAtaxi v2)

Order A Taxi is a premium, feature-rich desktop taxi booking application built with **Java Swing** and **SQLite**. It offers a seamless experience for passengers to book rides, drivers to manage their income, and admins to oversee the entire system.

## 🚀 Key Features

### 👤 Passenger Portal
- **Interactive Booking Wizard**: A sleek, multi-step process for selecting pickup/drop-off locations and vehicle types.
- **Real-time Fare Estimation**: Dynamic pricing based on distance, traffic factors, and vehicle category (Economy, Premium, XL).
- **Payment Integration**: Supports both Credit Card and Cash payments with a stylized UI.
- **Ride History**: Persistent log of all past rides with trip details and ratings.
- **Rating System**: Rate your driver and leave comments after every trip.
- **Customer Support**: Integrated ticket system to report issues or lost items.

### 🚕 Driver Dashboard
- **Request Management**: Real-time view of pending ride requests.
- **Ride Simulation**: High-fidelity simulation of the journey with progress tracking.
- **Automated Status Lifecycle**: Drivers are automatically marked as "Available" or "Busy" based on journey progress and estimated duration.
- **Earnings Tracker**: Detailed view of daily/monthly income and job history.

### 🛠️ Admin Panel
- **Driver Verification**: Review and approve/ban drivers.
- **System Stats**: Overview of active rides and total revenue.
- **Ticket Resolution**: Manage and resolve passenger support requests.

## 🛠️ Technology Stack
- **Language**: Java 17+
- **UI Framework**: Java Swing (with custom premium aesthetics)
- **Database**: SQLite (via JDBC)
- **Design Patterns**: Singleton (Database), Repository Pattern (Data Access), Service Layer Architecture.

## 📦 Installation & Setup

### Prerequisites
- **JDK 17** or higher.
- **SQLite JDBC Driver** (included in `lib/`).

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/OrderAtaxi.git
   ```
2. Ensure the following JARs are in your classpath:
   - `sqlite-jdbc-3.45.3.0.jar`
   - `slf4j-api-1.7.36.jar`
   - `slf4j-nop-1.7.36.jar`
   - `JMapViewer.jar`

## ▶️ How to Run
Run the main application class:
```bash
java -cp "lib/*;src" pack.TaxiFinalApp
```

## 🛡️ User Roles (Demo Credentials)
- **Passenger**: Register a new account via the UI.
- **Driver**: `driver` / `123`
- **Admin**: `admin` / `admin`

## 📄 License
This project was developed for academic purposes. Feel free to use and modify it for your own learning!


Created By Ertan
