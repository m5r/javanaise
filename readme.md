# javanaise

As part of the [distributed systems and applications course](http://lig-membres.imag.fr/boyer/html/Documents/cours/JAVANAISE/index.htm) at Université Grenoble Alpes, I built this distributed object cache.

You are on the **v2** branch, corresponding to the version using dynamic proxies. You can find the first version of this project within the [v1](https://github.com/m5r/javanaise/tree/v1) branch.

&nbsp;

In this version, it can handle :
- outages from the coordinator
- outages from the clients
- cache limit on the client

Using the stress test helper, the coordinator was tested against 5000 concurrent clients.

## Requirements

- JDK 8
- RMI Registry

## Quick start

1. Clone the repo — `git clone -b v1.0 git@github.com:m5r/javanaise.git`
2. Move to the project directory — `cd javanaise`
3. Compile it — `javac ./src/**/*.java -cp ./src -d ./build`
4. Run the coordinator — `java -cp ./build jvn.JvnCoordImpl`
5. Run the example client — `java -cp ./build irc.Irc`

## Stress test

1. Repeat steps 1 through 4 from the **Quick start**
2. Run the stresser — `java -cp ./build stress.StressTest`
