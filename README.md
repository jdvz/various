# Test tasks from various 

## org.example.CalculateCodeLines
Count lines with java code in text. 
Code is not validated, every line counts as a code, except empty lines, and commented lines.
#### approx ~1h in online editor (failed)

Examples
```java
import java.lang.String;

public class Test1 {
  public static void main(String ... args) {
    // print something
    System.out.println("Das ist die \"Kode\"");
  }
}
```
contains 6 lines of code
```java
import java.lang.String;
    test22("wow")
    // print something
    System/*.out.*/println("Das ist die \"Kode\"");
  }
 } 
}
```
No chance this code would pass compilation, but according to the initial requirements it contains 6 lines of code

So we got the problem in requirements, there is no explanation and noone to ask for the next cases
```java
System/*.out.*/println("Das ist die \"Kode\"
    and probably it can't be compiled, but
    ");
System.out./*println*/("""Das ist die \"Kode\"
    in text block that should not have text in the same lane
    with the triple quotes""");
```
but how could we judge it in calculation? There are two option I can imagine:

- to ignore invalid code.
- to ignore java rules
At first sight the first option looks promising, but let's imagine
```java
System.out./*println*/("Still literal
  and ignoring part of code"); System.out.println("valid text");
```
Since we ignore invalid code we'll have to compulsorily close the literal on the first line, and on the second we will get
```"); System.out.println("``` as String literal and corrupted end of line
The other solution ignore the syntax, but we do it anyway. Therefore, I hereby allow to use \r in a string literals for this particular solution.

By the way, the best solution I can imagine is to compile the code and with using of program transformation and reflection somehow calculate it. Not sure if this is possible, but it could be an interesting challenge. 