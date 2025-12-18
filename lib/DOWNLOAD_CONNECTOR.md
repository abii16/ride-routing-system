# MySQL Connector Download Instructions

## You need the MySQL Connector/J JAR file to run this system.

### Option 1: Direct Download
1. Go to: https://dev.mysql.com/downloads/connector/j/
2. Select "Platform Independent"
3. Download the ZIP file
4. Extract `mysql-connector-j-9.2.0.jar` (or similar version)
5. Place it in this `lib/` directory

### Option 2: Maven Central (if you have Maven)
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.2.0</version>
</dependency>
```

### Option 3: Direct Link (may change)
Try: https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.2.0/mysql-connector-j-9.2.0.jar

### Verification
After downloading, this directory should contain:
```
lib/
  └── mysql-connector-j-9.2.0.jar  (size: ~2.5 MB)
```

### Alternative Versions
If version 9.2.0 is not available, you can use:
- 8.4.0
- 8.3.0
- 8.0.33

Just update the version number in build.bat and start-*.bat scripts.

## Why do we need this?
The MySQL Connector/J is the official JDBC driver for MySQL. It allows Java applications to connect to MySQL databases. Without it, you'll get:
```
ClassNotFoundException: com.mysql.cj.jdbc.Driver
```
