# Queryable Encryption (QE) demo

## Steps

1. Build the executable jar:
   ```
   mvn clean package
   ```
   
2. Run the demo:
   ```
   java -jar bin/qe-demo.jar
   ```
   
## Dev

- The Java project is in `java/` folder
- Modify config in `java/src/main/resources/.env`
- Uses maven shade plugin to compile executable jar in `bin/` directory