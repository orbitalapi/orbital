db = db.getSiblingDB('user_management');

db.createUser({
   user: 'test_container',
   pwd: 'test_container',
   roles: [
      { role: 'readWrite', db: 'user_management' }
   ]
});

db.users.insertOne({
   firstName: "Harry",
   lastName: "Potter",
   nickname: "The Slayer",
   email: "harry.potter@gmail.com",
   age: 11
});

db.users.insertOne({
   firstName: "Albus",
   lastName: "Dumbledore",
   nickname: "",
   email: "albus.dumbledore@gmail.com",
   age: 110
});

db.users.insertOne({
   firstName: "Tom",
   lastName: "Riddle",
   nickname: "Lord Voldemort",
   email: "tom.riddle@gmail.com",
   age: 65
});

db.users.insertOne({
   firstName: "George",
   lastName: "Weasley",
   nickname: "The holy",
   email: "george.weasley@gmail.com",
   age: 14
});

db.users.insertOne({
   firstName: "Severus",
   lastName: "Snape",
   nickname: "Half-Blood Prince",
   email: "severus.snape@gmail.com",
   age: 31
});

db.users.insertOne({
   firstName: "Minerva",
   lastName: "McGonagall",
   nickname: "",
   email: "minerva.mcgonagall@gmail.com",
   age: 57
});
