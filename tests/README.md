# Test Cases

This folder contains the manual test cases used to validate the refactoring tool.

Each subfolder (`ejemplo1`, `ejemplo2`, ...) is an independent Eclipse Java project.  
They can be imported into an Eclipse workspace and executed to verify how the plugin 
refactors different control-flow structures (`break`, `continue`, `for`, `foreach`, etc.).

## Structure

ejemplo3  --> Simple break in a while loop
ejemplo9  --> Multiple break statements belonging to the same while loop
ejemplo11 --> Nested while loops, each containing a break
ejemplo16 --> Refactoring of a do-while loop
ejemplo18 --> Refactoring of an enhanced for-each loop with lists and arrays
ejemplo19 --> Refactoring of a continue statement inside a while loop
ejemplo37 --> Refactoring of break and continue inside a while loop nested in a do-while loop
ejemplo39 --> break statements inside for and while loops
ejemplo44 --> while loop with exception handling: try-catch-finally
ejemplo45 --> while loop with a break and a switch containing break statements


## Usage

1. Import the desired test project into your Eclipse workspace.
2. Apply the refactoring tool (menu: *Refactor → Replace break/continue*).
3. Compare the original code against the refactored output.
