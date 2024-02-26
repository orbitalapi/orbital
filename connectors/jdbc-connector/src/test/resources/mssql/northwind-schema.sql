create database Northwind;
USE Northwind;

create table Categories (
                                         CategoryID int primary key not null,
                                         CategoryName nvarchar(15) not null,
                                         Description ntext,
                                         Picture image
);
create index CategoryName on Categories (CategoryName);

create table CustomerDemographics (
                                     CustomerTypeID nchar(10) primary key not null,
                                     CustomerDesc ntext
);


create table Customers (
                                        CustomerID nchar(5) primary key not null,
                                        CompanyName nvarchar(40) not null,
                                        ContactName nvarchar(30),
                                        ContactTitle nvarchar(30),
                                        Address nvarchar(60),
                                        City nvarchar(15),
                                        Region nvarchar(15),
                                        PostalCode nvarchar(10),
                                        Country nvarchar(15),
                                        Phone nvarchar(24),
                                        Fax nvarchar(24)
);

create table CustomerCustomerDemo (
                                     CustomerID nchar(5) not null,
                                     CustomerTypeID nchar(10) not null,
                                     primary key (CustomerID, CustomerTypeID),
                                     foreign key (CustomerTypeID) references CustomerDemographics (CustomerTypeID),
                                     foreign key (CustomerID) references Customers (CustomerID)
);

create index CompanyName on Customers (CompanyName);
create index Region on Customers (Region);
create index PostalCode on Customers (PostalCode);
create index City on Customers (City);

create table Employees (
                                        EmployeeID int primary key not null,
                                        LastName nvarchar(20) not null,
                                        FirstName nvarchar(10) not null,
                                        Title nvarchar(30),
                                        TitleOfCourtesy nvarchar(25),
                                        BirthDate datetime,
                                        HireDate datetime,
                                        Address nvarchar(60),
                                        City nvarchar(15),
                                        Region nvarchar(15),
                                        PostalCode nvarchar(10),
                                        Country nvarchar(15),
                                        HomePhone nvarchar(24),
                                        Extension nvarchar(4),
                                        Photo image,
                                        Notes ntext,
                                        ReportsTo int,
                                        PhotoPath nvarchar(255),
                                        foreign key (ReportsTo) references Employees (EmployeeID)
);
create index PostalCode on Employees (PostalCode);
create index LastName on Employees (LastName);

create table Shippers (
                         ShipperID int primary key not null,
                         CompanyName nvarchar(40) not null,
                         Phone nvarchar(24)
);


create table Orders (
                                     OrderID int primary key not null,
                                     CustomerID nchar(5),
                                     EmployeeID int,
                                     OrderDate datetime,
                                     RequiredDate datetime,
                                     ShippedDate datetime,
                                     ShipVia int,
                                     Freight money default ((0)),
                                     ShipName nvarchar(40),
                                     ShipAddress nvarchar(60),
                                     ShipCity nvarchar(15),
                                     ShipRegion nvarchar(15),
                                     ShipPostalCode nvarchar(10),
                                     ShipCountry nvarchar(15),
                                     foreign key (CustomerID) references Customers (CustomerID),
                                     foreign key (EmployeeID) references Employees (EmployeeID),
                                     foreign key (ShipVia) references Shippers (ShipperID)
);
create index CustomersOrders on Orders (CustomerID);
create index ShippersOrders on Orders (ShipVia);
create index EmployeesOrders on Orders (EmployeeID);
create index ShippedDate on Orders (ShippedDate);
create index ShipPostalCode on Orders (ShipPostalCode);
create index CustomerID on Orders (CustomerID);
create index EmployeeID on Orders (EmployeeID);
create index OrderDate on Orders (OrderDate);

create table Suppliers (
                          SupplierID int primary key not null,
                          CompanyName nvarchar(40) not null,
                          ContactName nvarchar(30),
                          ContactTitle nvarchar(30),
                          Address nvarchar(60),
                          City nvarchar(15),
                          Region nvarchar(15),
                          PostalCode nvarchar(10),
                          Country nvarchar(15),
                          Phone nvarchar(24),
                          Fax nvarchar(24),
                          HomePage ntext
);
create index CompanyName on Suppliers (CompanyName);
create index PostalCode on Suppliers (PostalCode);


create table Products (
                                       ProductID int primary key not null,
                                       ProductName nvarchar(40) not null,
                                       SupplierID int,
                                       CategoryID int,
                                       QuantityPerUnit nvarchar(20),
                                       UnitPrice money default ((0)),
                                       UnitsInStock smallint default ((0)),
                                       UnitsOnOrder smallint default ((0)),
                                       ReorderLevel smallint default ((0)),
                                       Discontinued bit default ((0)) not null,
                                       foreign key (CategoryID) references Categories (CategoryID),
                                       foreign key (SupplierID) references Suppliers (SupplierID)
);
create index CategoryID on Products (CategoryID);
create index SuppliersProducts on Products (SupplierID);
create index SupplierID on Products (SupplierID);
create index ProductName on Products (ProductName);
create index CategoriesProducts on Products (CategoryID);


create table [Order Details] (
                                OrderID int not null,
                                ProductID int not null,
                                UnitPrice money default ((0)) not null,
   Quantity smallint default ((1)) not null,
   Discount real default ((0)) not null,
   primary key (OrderID, ProductID),
   foreign key (OrderID) references Orders (OrderID),
   foreign key (ProductID) references Products (ProductID)
   );
create index OrdersOrder_Details on [Order Details] (OrderID);
create index ProductsOrder_Details on [Order Details] (ProductID);
create index ProductID on [Order Details] (ProductID);
create index OrderID on [Order Details] (OrderID);


create table Region (
                                     RegionID int primary key not null,
                                     RegionDescription nchar(50) not null
);



create table Territories (
                                          TerritoryID nvarchar(20) primary key not null,
                                          TerritoryDescription nchar(50) not null,
                                          RegionID int not null,
                                          foreign key (RegionID) references Region (RegionID)
);

create table EmployeeTerritories (
                                    EmployeeID int not null,
                                    TerritoryID nvarchar(20) not null,
                                    primary key (EmployeeID, TerritoryID),
                                    foreign key (EmployeeID) references Employees (EmployeeID),
                                    foreign key (TerritoryID) references Territories (TerritoryID)
);
