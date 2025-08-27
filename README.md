This Eclipse plugin performs an **automatic refactoring of Java code** to eliminate the use of `break` and `continue` statements inside loops (`while`, `for`, `do-while`, `enhanced for`).  
The refactoring introduces auxiliary boolean variables (`stay`, `keep`) to preserve the original semantics while making the code more structured and suitable for further transformations.

## Features
- Replaces `break` statements with a `stay` flag.
- Replaces `continue` statements with a `keep` flag.
- Converts `for` and `enhanced for` loops into equivalent `while` loops.
- Groups consecutive `if` statements with the same condition into a single block.
- Preserves program semantics.

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/andy2078/java-break-continue-refactor.git
2.	Import the project into Eclipse as an existing project.
3.	Run or debug as an Eclipse plugin.

## Usage
•	Select a Java file in Eclipse.
•	From the context menu, choose:
•	Refactor → Break/Continue Refactor → Apply
•	The code will be automatically transformed.

## Example
Before:
while (condition) {
    if (x > 0) {
        break;
    }
    y++;
}

After:
boolean stay = true;
while (stay && (condition)) {
    if (x > 0) {
        stay = false;
    }
    if (stay) {
        y++;
    }
}

