package rocketlearn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Every question in the game, grouped into the three difficulty tiers the
 * planets draw from. Each question carries a topic so the game can report
 * which subject the player keeps missing.
 */
class QuestionBank {

    static final String BASICS = "Programming Basics";
    static final String FLOWCHARTS = "Flowcharts";
    static final String CONDITIONALS = "Conditionals";
    static final String SWITCH = "Switch Statements";
    static final String LOOPS = "Loops";
    static final String ARRAYS = "Arrays";
    static final String OPERATORS = "Operators";
    static final String TYPES = "Variables & Types";
    static final String ALGORITHMS = "Algorithms";

    static final List<Question> TIER0 = Arrays.asList(
        new Question("Which statement best describes programming?", new String[]{"Design, write & test code", "Typing code fast", "Installing software", "Binary to decimal"}, 0,
            "Programming is the whole process of designing, writing and testing code - not just typing it quickly.", BASICS),
        new Question("In a flowchart, which symbol evaluates a condition (Yes/No, True/False)?", new String[]{"Diamond", "Oval", "Rectangle", "Parallelogram"}, 0,
            "The diamond is the decision symbol: one path goes in and two paths come out (Yes/No).", FLOWCHARTS),
        new Question("Which of the following is a low-level programming language?", new String[]{"Assembly Language", "Python", "Java", "JavaScript"}, 0,
            "Assembly is low-level because its instructions map almost directly onto the CPU's own instructions.", BASICS),
        new Question("The if statement executes its block of code:", new String[]{"Only when true", "Always", "Only when false", "Once, then never again"}, 0,
            "An if block runs only when its condition evaluates to true; otherwise it is skipped entirely.", CONDITIONALS),
        new Question("In Java, what is the difference between = and ==?", new String[]{"= assigns, == compares", "No difference", "= compares, == assigns", "Only used in switch"}, 0,
            "A single = stores a value in a variable; == compares two values and produces true or false.", OPERATORS)
    );

    static final List<Question> TIER1 = Arrays.asList(
        new Question("In a flowchart, which symbol marks the start or end of a process?", new String[]{"Oval", "Rectangle", "Diamond", "Parallelogram"}, 0,
            "The oval, also called the terminator, marks where a process begins and where it ends.", FLOWCHARTS),
        new Question("In a flowchart, which symbol reads input or displays output?", new String[]{"Parallelogram", "Oval", "Rectangle", "Diamond"}, 0,
            "The parallelogram is the input/output symbol - reading data in, or displaying results out.", FLOWCHARTS),
        new Question("In a flowchart, which symbol performs a calculation or instruction?", new String[]{"Rectangle", "Oval", "Parallelogram", "Diamond"}, 0,
            "The rectangle is the process symbol: a calculation or instruction to be carried out.", FLOWCHARTS),
        new Question("In a flowchart, what shows the direction of execution?", new String[]{"An arrow", "A diamond", "An oval", "A rectangle"}, 0,
            "Arrows, or flow lines, show the order in which the steps are carried out.", FLOWCHARTS),
        new Question("age=16; if age>=18 print \"adult\" else print \"minor\"; then always print \"Thank you!\". What prints?", new String[]{"Minor, then Thank you", "Adult, then Thank you", "Minor only", "Thank you only"}, 0,
            "16 is not >= 18, so the else branch prints 'minor'. The last line sits outside the if, so it always runs.", CONDITIONALS),
        new Question("In a switch statement, what is the purpose of the default case?", new String[]{"Runs when no case matches", "Always runs first", "Removes need for break", "Required in every switch"}, 0,
            "default is the catch-all branch: it runs only when none of the case values match.", SWITCH),
        new Question("Which loop is guaranteed to execute its body at least once, even if its condition starts false?", new String[]{"do-while loop", "for loop", "while loop", "for-each loop"}, 0,
            "A do-while checks its condition after the body, so the body always runs at least one time.", LOOPS),
        new Question("The three parts inside a for loop header, in the correct order, are:", new String[]{"init; condition; update", "condition; init; update", "update; condition; init", "init; update; condition"}, 0,
            "Initialization runs once, the condition is checked before each pass, and the update runs after each pass.", LOOPS),
        new Question("int[] values={5,10,15,20}; a loop prints values[i]*2 for each i. What prints, in order?", new String[]{"10, 20, 30, 40", "5, 10, 15, 20", "10, 20, 30, 40, 80", "20, 15, 10, 5"}, 0,
            "The loop visits each element in order and doubles it, so 5, 10, 15, 20 become 10, 20, 30, 40.", ARRAYS),
        new Question("What is the key difference between break and continue in a loop?", new String[]{"break stops all; continue skips one", "break skips one; continue stops all", "Both stop the loop", "Both only skip one"}, 0,
            "break exits the loop entirely; continue skips the rest of the current pass and moves to the next one.", LOOPS)
    );

    static final List<Question> TIER0_EXTRA = Arrays.asList(
        new Question("Which of these is a valid Java variable name?", new String[]{"totalScore", "2total", "total-score", "class"}, 0,
            "Names cannot start with a digit, cannot contain hyphens, and cannot be reserved words such as class.", TYPES),
        new Question("What does the % operator return?", new String[]{"The remainder of a division", "The result of a division", "The square of a number", "A random number"}, 0,
            "The modulus operator gives what is left over after dividing, so 7 % 2 is 1.", OPERATORS),
        new Question("Which keyword declares a variable that cannot be reassigned?", new String[]{"final", "static", "void", "public"}, 0,
            "A final variable may be assigned once; any later reassignment is a compile error.", TYPES),
        new Question("What is the correct way to print text to the console in Java?", new String[]{"System.out.println(\"text\");", "print(\"text\")", "console.log(\"text\")", "echo \"text\""}, 0,
            "System.out.println prints its argument and then moves the cursor to a new line.", BASICS),
        new Question("Which data type would you use to store a true/false value?", new String[]{"boolean", "int", "String", "char"}, 0,
            "boolean is the type that holds exactly two values: true and false.", TYPES),
        new Question("In Java, // starts:", new String[]{"A single-line comment", "A loop", "A method", "An error"}, 0,
            "Two slashes begin a comment that runs to the end of the line, and the compiler ignores it.", BASICS),
        new Question("What does the ++ operator do to a variable?", new String[]{"Increases it by 1", "Decreases it by 1", "Doubles it", "Sets it to zero"}, 0,
            "The increment operator adds one, so x++ is shorthand for x = x + 1.", OPERATORS),
        new Question("Which loop is best when you know exactly how many times to repeat something?", new String[]{"for loop", "while loop", "do-while loop", "if statement"}, 0,
            "Use a for loop when the repetition count is known in advance; a while loop suits an unknown count.", LOOPS),
        new Question("int[] scores = {90, 85, 70}; what is scores[1]?", new String[]{"85", "90", "70", "3"}, 0,
            "Array indexes start at 0, so index 1 is the second element, which is 85.", ARRAYS),
        new Question("Which of these correctly declares an integer variable named count?", new String[]{"int count;", "count int;", "integer count;", "var count = int;"}, 0,
            "A declaration names the type first and then the variable: int count;", TYPES)
    );

    static final List<Question> TIER2 = Arrays.asList(
        new Question("An algorithm's step keeps repeating and never stops. Which characteristic does it violate?", new String[]{"Finiteness", "Well-defined inputs", "Language independence", "Feasibility"}, 0,
            "Finiteness requires an algorithm to end after a limited number of steps, so a loop that never stops breaks it.", ALGORITHMS),
        new Question("Which is a real advantage of high-level languages (Java, Python) over low-level ones?", new String[]{"Easier & faster to build", "Faster execution speed", "Full hardware control", "No compiler needed"}, 0,
            "High-level languages read closer to human language, so programs are quicker to write and easier to maintain.", BASICS),
        new Question("In pseudocode that reads three quiz scores and shows Passed/Failed, what condition shows \"Passed\"?", new String[]{"average >= 75", "average > 75", "average >= 60", "all scores >= 75"}, 0,
            "The rule tests the average of the three scores reaching 75, not each individual score.", CONDITIONALS),
        new Question("grade=95; if(grade>=75) print Passed; else if(grade>=90) print Excellent; else print Failed. What prints?", new String[]{"Passed", "Excellent", "Failed", "Won't compile"}, 0,
            "if-else-if stops at the first true test. 95 >= 75 is true, so the later Excellent branch is never reached.", CONDITIONALS),
        new Question("switch(2): case1 \"Add\" (no break), case2 \"View\" (no break), case3 \"Delete\"+break, default \"Invalid\"; then always print \"Done\". Full output?", new String[]{"View, Delete, Done", "View, Done", "Add, View, Delete, Done", "Invalid, Done"}, 0,
            "Without a break, case 2 falls through: View prints, then Delete prints and breaks. Done always runs afterwards.", SWITCH)
    );

    static final List<Question> TIER3 = Arrays.asList(
        new Question("When should you use logical operators (&&) instead of a nested if?", new String[]{"Same outcome either way", "Different message each", "Only inside a switch", "Only for String values"}, 0,
            "Both forms give the same result; && is simply shorter when you only need one combined outcome.", CONDITIONALS),
        new Question("A loop uses 'continue' to skip odd numbers in {4,7,10,13,16} before printing. What prints, in order?", new String[]{"4, 10, 16", "4, 7, 10, 13, 16", "7, 13", "Nothing prints"}, 0,
            "continue skips the odd values 7 and 13 before the print runs, leaving 4, 10 and 16.", LOOPS),
        new Question("i=1,sum=0; while(i<=5){sum+=i; i++;} print sum and i. What are the two final values?", new String[]{"Sum: 15, i: 6", "Sum: 15, i: 5", "Sum: 10, i: 5", "Sum: 20, i: 6"}, 0,
            "The loop adds 1+2+3+4+5 = 15, and i must reach 6 before the condition i <= 5 finally fails.", LOOPS),
        new Question("A while loop starts count=10 (loops while count<5); a do-while starts n=10, runs its body once, then checks n<5. Full output?", new String[]{"Only Do-While: 10", "Only While: 10", "Both print 10", "Nothing prints"}, 0,
            "The while body never runs because 10 < 5 is false immediately, but the do-while runs its body first.", LOOPS)
    );

    static final List<Question> TIER2_EXTRA = Arrays.asList(
        new Question("int x=7; if(x%2==0) print \"Even\"; else print \"Odd\". What prints?", new String[]{"Odd", "Even", "7", "Nothing prints"}, 0,
            "7 % 2 is 1, not 0, so the condition is false and the else branch runs.", CONDITIONALS),
        new Question("for(int i=0;i<5;i++) print i;. What is the last value printed?", new String[]{"4", "5", "0", "3"}, 0,
            "The loop continues only while i is less than 5, so the last value printed is 4.", LOOPS),
        new Question("Which operator checks that BOTH conditions must be true?", new String[]{"&&", "||", "!", "=="}, 0,
            "The AND operator is true only when both sides are true; || needs just one side to be true.", OPERATORS),
        new Question("int total=0; for(int i=1;i<=4;i++) total+=i; What is total after the loop?", new String[]{"10", "4", "6", "15"}, 0,
            "The loop adds 1 + 2 + 3 + 4, which is 10.", LOOPS),
        new Question("A switch statement is missing a break after case 1. If case 1 matches, what happens?", new String[]{"Execution falls through into case 2", "Only case 1 runs", "The program won't compile", "default runs instead"}, 0,
            "Without break, control falls through and keeps executing the statements in the following cases.", SWITCH),
        new Question("Which loop should you use to process every element of an array exactly once, in order?", new String[]{"A for loop indexed 0 to length-1", "A while(true) loop", "A do-while loop with no condition", "A switch statement"}, 0,
            "Counting from 0 up to length-1 visits every element exactly one time, in order.", ARRAYS),
        new Question("if(age>=13 && age<20) print \"Teen\"; else print \"Not a teen\". For age=13, what prints?", new String[]{"Teen", "Not a teen", "Nothing prints", "Error"}, 0,
            "13 >= 13 is true and 13 < 20 is true, so both sides of the && hold.", CONDITIONALS),
        new Question("int count=0; while(count<3){ print count; count++; } What is printed, in order?", new String[]{"0, 1, 2", "1, 2, 3", "0, 1, 2, 3", "3, 2, 1"}, 0,
            "count is printed before each increment, so it prints 0, 1 and 2, then stops once it reaches 3.", LOOPS),
        new Question("Which condition correctly checks if n is between 1 and 10, inclusive?", new String[]{"n >= 1 && n <= 10", "n > 1 && n < 10", "n >= 1 || n <= 10", "n == 1 to 10"}, 0,
            "Inclusive means the endpoints count, so both comparisons need 'or equal to' and must be joined by &&.", CONDITIONALS),
        new Question("A nested for loop's outer loop runs 3 times, and the inner loop runs 2 times per pass. How many total times does the inner loop's body execute?", new String[]{"6", "5", "3", "2"}, 0,
            "The inner body runs the inner count once for every outer pass, so 3 x 2 = 6.", LOOPS)
    );

    static final List<Question> TIER4 = Arrays.asList(
        new Question("What is stepwise refinement (top-down design) used for?", new String[]{"Break big problems down", "Combine finished programs", "Speed up a program", "Remove bugs from code"}, 0,
            "Top-down design splits one large problem into smaller sub-problems that are easier to solve one at a time.", ALGORITHMS),
        new Question("How many decision (diamond) symbols are needed in a flowchart that finds the largest of three numbers?", new String[]{"Two", "One", "Three", "Four"}, 0,
            "One decision compares the first two numbers, and a second compares that winner against the third.", FLOWCHARTS),
        new Question("A flawed 'algorithm' says \"compute the result\" then \"if the answer looks correct, stop, else go back.\" Which two characteristics does it violate?", new String[]{"Clear steps & Finiteness", "Feasibility & Language ind.", "Inputs & Feasibility", "Language ind. & Finiteness"}, 0,
            "'Compute the result' is not a precise step, and 'go back' with no guaranteed exit means it may never finish.", ALGORITHMS),
        new Question("switch grade/10 for grade=83: case 8 yields \"Very Good\" if grade>=85, else \"Good\". What prints?", new String[]{"Good", "Very Good", "Excellent", "Passed"}, 0,
            "Integer division makes 83 / 10 equal 8, so case 8 runs; 83 is not >= 85, so the else gives Good.", SWITCH),
        new Question("After fixing 3 bugs (assignment vs comparison, wrong branch order, missing braces) in a grading program, what prints for score=95?", new String[]{"Perfect, then End", "Excellent, then End", "Perfect only", "Won't compile"}, 0,
            "With == used for the test, the highest branch checked first, and the braces grouping the block, 95 reaches Perfect and the final line still runs.", CONDITIONALS)
    );

    static final List<Question> TIER5 = Arrays.asList(
        new Question("In a scholarship check using nested if, why is nested if better than one condition joined by &&?", new String[]{"Shows the exact failed reason", "No booleans allowed in &&", "Nested if runs faster", "&& allows only 2 conditions"}, 0,
            "Nested ifs let each failed condition report its own message; a single && only tells you the whole test failed.", CONDITIONALS),
        new Question("for(i=1..3){ for(j=1..3){ if(j==2) break; print i-j; } }. What prints?", new String[]{"1-1, 2-1, 3-1", "1-1, 1-2, 1-3", "Nothing prints", "1-1, 2-1, 2-2, 3-1"}, 0,
            "The inner loop always breaks once j reaches 2, so only j == 1 prints on each of the three outer passes.", LOOPS),
        new Question("A program searches an array for a name with an indexed for loop and break. Why use an indexed for loop instead of for-each?", new String[]{"Reports the index found", "for-each can't use break", "for-each is slower", "Arrays need index loops"}, 0,
            "The index variable is what lets you report the position where the name was found; for-each gives only the value.", ARRAYS),
        new Question("For each value in {3,8,12,7,20,5,30}: skip odd values, stop entirely once a value>15 appears, else add to total & print it. Full output?", new String[]{"Added 8, 12; Total: 20", "Added 8,12,20; Total: 40", "Total: 75", "Added 8,12,5; Total: 25"}, 0,
            "The odd values are skipped, so 8 and 12 are added; 20 is over 15, so the loop stops before adding it.", LOOPS)
    );

    static final List<Question> TIER4_EXTRA = Arrays.asList(
        new Question("for(int i=1;i<=3;i++){ for(int j=1;j<=3;j++){ if(i==j) continue; print i+\",\"+j; } }. How many pairs are printed?", new String[]{"6", "9", "3", "0"}, 0,
            "There are 9 pairs in total, and the 3 where i equals j are skipped, leaving 6.", LOOPS),
        new Question("An algorithm to sort a list is described only as \"put them in the right order.\" Which characteristic does it violate?", new String[]{"Well-defined, clear steps", "Finiteness", "Feasibility", "Language independence"}, 0,
            "'The right order' is not a precise instruction, so the steps are not well defined enough to follow.", ALGORITHMS),
        new Question("int n=5, result=1; for(int i=1;i<=n;i++) result*=i; What is result after the loop (5 factorial)?", new String[]{"120", "20", "25", "5"}, 0,
            "The loop multiplies 1 x 2 x 3 x 4 x 5, which is 120.", LOOPS),
        new Question("A nested if-else assigns a letter grade (A/B/C/F) but checks the F condition before the A condition. What happens to a score of 95?", new String[]{"It may be misclassified as F", "It always still gets A correctly", "The program won't compile", "It throws a runtime error"}, 0,
            "if-else-if stops at the first true test, so a broad F condition checked first can catch scores that should be A.", CONDITIONALS),
        new Question("while(true){ if(x>100) break; x+=10; } If x starts at 85, how many times is x increased before the loop breaks?", new String[]{"2", "1", "3", "10"}, 0,
            "85 becomes 95 and then 105; the next check finds 105 > 100 and breaks, so x was increased twice.", LOOPS),
        new Question("A loop searching for a value uses 'break' once found instead of just letting the loop finish. Why is break more efficient here?", new String[]{"It stops scanning once the answer is found", "It skips only the current iteration", "It restarts the loop from zero", "It has no effect on performance"}, 0,
            "break ends the search immediately instead of pointlessly scanning the remaining elements.", LOOPS),
        new Question("for(int i=10;i>0;i-=2) print i;. What is the last value printed?", new String[]{"2", "0", "1", "10"}, 0,
            "The values printed are 10, 8, 6, 4 and 2; the next value 0 fails the i > 0 test.", LOOPS),
        new Question("Two separate (not nested) for loops each declare their own for(int i=0; ...). What happens to the second loop's i?", new String[]{"It starts fresh, independent of the first loop", "It continues from the first loop's final value", "The program won't compile due to duplicate declaration", "It causes an infinite loop"}, 0,
            "A variable declared in a for header exists only inside that loop, so the second loop's i is entirely separate.", TYPES),
        new Question("An algorithm has clear steps, finite steps, and defined inputs/outputs, but needs more memory than the computer has. Which characteristic does it fail?", new String[]{"Feasibility", "Finiteness", "Well-defined inputs", "Language independence"}, 0,
            "Feasibility means the algorithm must be practical to run with the resources actually available.", ALGORITHMS),
        new Question("int[] data={4,-2,9,-7,3}; a loop sums only the positive numbers, using continue to skip negatives. What is the final sum?", new String[]{"16", "7", "-9", "23"}, 0,
            "The negatives are skipped, so the total is 4 + 9 + 3, which is 16.", ARRAYS)
    );

    /**
     * Fill-in-the-blank questions: a snippet with a gap, and the four choices
     * are the tokens that might fill it.
     *
     * The wrong choices are deliberately things that would not compile or would
     * change the result, rather than near-synonyms - a distractor that is also
     * correct teaches the wrong lesson. Every snippet is kept to a few short
     * lines so it fits the question panel without the font having to shrink.
     */
    static final List<Question> TIER0_CODE = Arrays.asList(
        new Question("The file is called Main.java. What belongs in the blank?",
            code("""
                public class [blank] {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }"""),
            new String[]{"Main", "main", "class", "JavaClass"}, 0,
            "A public class must share the name of its file, spelled exactly - Main.java has to hold 'public class Main'.", BASICS),
        new Question("Which method prints a line of text to the screen?",
            "System.out." + Question.BLANK + "(\"Hello\");",
            new String[]{"println", "printline", "Println", "writeline"}, 0,
            "It is println - 'print line'. Java is case sensitive, so Println with a capital P will not compile.", BASICS),
        new Question("Which type holds a whole number?",
            Question.BLANK + " age = 18;",
            new String[]{"int", "Int", "integer", "number"}, 0,
            "int is the keyword for a whole number. Java keywords are all lower case, so Int and integer are not types.", TYPES),
        new Question("Which keyword starts a decision?",
            code("""
                [blank] (score >= 75) {
                    System.out.println("Pass");
                }"""),
            new String[]{"if", "when", "check", "then"}, 0,
            "if is the keyword that tests a condition and runs its block only when the condition is true.", CONDITIONALS),
        new Question("Which operator compares two values here?",
            code("""
                if (pin [blank] 1234) {
                    System.out.println("Welcome");
                }"""),
            new String[]{"==", "=", "=>", "equals"}, 0,
            "== compares and gives true or false. A single = assigns, which is why it does not belong in a condition.", OPERATORS),
        new Question("Which type holds a piece of text?",
            Question.BLANK + " name = \"Ana\";",
            new String[]{"String", "string", "char", "Text"}, 0,
            "String, with a capital S, holds text. char holds only one character, and string in lower case is not a type.", TYPES)
    );

    static final List<Question> TIER2_CODE = Arrays.asList(
        new Question("Which comparison makes this print 0, 1, 2, 3, 4?",
            code("""
                for (int i = 0; i [blank] 5; i++) {
                    System.out.println(i);
                }"""),
            new String[]{"<", "<=", ">", ">="}, 0,
            "i < 5 stops once i reaches 5, so the last value printed is 4. i <= 5 would print 5 as well.", LOOPS),
        new Question("How do you ask an array how many elements it holds?",
            code("""
                int[] nums = {4, 8, 15};
                for (int i = 0; i < nums.[blank]; i++) {
                    System.out.println(nums[i]);
                }"""),
            new String[]{"length", "length()", "size", "size()"}, 0,
            "An array uses length with no brackets - it is a field, not a method. size() belongs to lists, not arrays.", ARRAYS),
        new Question("Which index prints 85?",
            code("""
                int[] scores = {90, 85, 70};
                System.out.println(scores[[blank]]);"""),
            new String[]{"1", "2", "0", "85"}, 0,
            "Array indexes start at 0, so 90 sits at 0 and 85 sits at 1.", ARRAYS),
        new Question("What keeps this from falling through into default?",
            code("""
                switch (day) {
                    case 1:
                        System.out.println("Monday");
                        [blank];
                    default:
                        System.out.println("Other");
                }"""),
            new String[]{"break", "continue", "stop", "end"}, 0,
            "break leaves the switch. Without it execution falls through and the default branch runs too.", SWITCH),
        new Question("Which keyword starts a loop that always runs once?",
            code("""
                int i = 0;
                [blank] {
                    System.out.println(i);
                    i++;
                } while (i < 3);"""),
            new String[]{"do", "loop", "repeat", "until"}, 0,
            "A do-while puts its condition at the bottom, so the body has already run once before the test happens.", LOOPS),
        new Question("Which keyword creates the array?",
            "int[] nums = " + Question.BLANK + " int[5];",
            new String[]{"new", "create", "make", "alloc"}, 0,
            "new asks for the memory the array needs. It is the same keyword used to build any object.", ARRAYS)
    );

    static final List<Question> TIER4_CODE = Arrays.asList(
        new Question("Which operator tests whether n divides evenly by 2?",
            code("""
                if (n [blank] 2 == 0) {
                    System.out.println("Even");
                }"""),
            new String[]{"%", "/", "*", "//"}, 0,
            "% is the remainder operator. A remainder of 0 when dividing by 2 is exactly what makes a number even.", OPERATORS),
        new Question("How do you test a second condition after the first fails?",
            code("""
                if (g >= 90) {
                    System.out.println("A");
                } [blank] (g >= 80) {
                    System.out.println("B");
                }"""),
            new String[]{"else if", "elif", "else", "or if"}, 0,
            "else if carries its own condition. A plain else takes no condition, and elif belongs to other languages.", CONDITIONALS),
        new Question("Which type holds a value with a decimal point?",
            Question.BLANK + " price = 19.99;",
            new String[]{"double", "int", "float", "decimal"}, 0,
            "A decimal literal like 19.99 is a double by default. Assigning it to a float needs 19.99f, so float alone will not compile.", TYPES),
        new Question("Which keyword starts a loop that checks before it runs?",
            code("""
                int n = 5;
                [blank] (n > 0) {
                    System.out.println(n);
                    n--;
                }"""),
            new String[]{"while", "do", "loop", "until"}, 0,
            "while tests its condition first, so if the condition starts false the body never runs at all.", LOOPS),
        new Question("Which method reads a whole number the user types?",
            code("""
                Scanner input = new Scanner(System.in);
                int age = input.[blank]();"""),
            new String[]{"nextInt", "nextInteger", "readInt", "getInt"}, 0,
            "Scanner names its readers after the type: nextInt, nextDouble, nextLine.", BASICS),
        new Question("Which statement skips the rest of this pass and starts the next one?",
            code("""
                for (int n : values) {
                    if (n < 0) {
                        [blank];
                    }
                    total += n;
                }"""),
            new String[]{"continue", "break", "skip", "return"}, 0,
            "continue jumps straight to the next pass, so the negative numbers never reach the line that adds to total. break would abandon the loop entirely.", LOOPS)
    );

    /**
     * True-or-false statements and find-the-bug snippets. A true/false
     * question flies two asteroids; a find-the-bug one numbers its snippet and
     * offers the wrong line among its neighbours. Each bug is one a beginner
     * really makes, and the explanation says what it would actually do.
     */
    static final List<Question> TIER0_MIXED = Arrays.asList(
        Question.trueFalse("True or false: Java is case sensitive, so total and Total are two different variables.", null, true,
            "Java treats upper and lower case as different letters, so total and Total name two separate variables.", TYPES),
        Question.trueFalse("True or false: an int variable can hold the value 3.5.", null, false,
            "int holds whole numbers only. A value with a decimal point like 3.5 needs a double.", TYPES),
        Question.trueFalse("True or false: the else block runs when the if condition is true.", null, false,
            "else is the other path: it runs only when the if condition is false.", CONDITIONALS),
        Question.findBug("This will not compile. Which line has the bug?",
            code("""
                int age = 15;
                if (age >= 13) {
                    System.out.println("Teen")
                }"""),
            3, "Every statement ends with a semicolon. Line 3 is missing its ; so Java stops with \"';' expected\".", BASICS),
        Question.findBug("This should check whether x is 5. Which line has the bug?",
            code("""
                int x = 5;
                if (x = 5) {
                    System.out.println("five");
                }"""),
            2, "x = 5 assigns rather than compares, and an int is not true or false, so it will not compile. Comparing needs ==.", OPERATORS),
        Question.findBug("This crashes when it runs. Which line has the bug?",
            code("""
                int[] nums = new int[3];
                nums[0] = 10;
                nums[3] = 30;"""),
            3, "An array of length 3 has indexes 0, 1 and 2. nums[3] is past the end, so line 3 throws an ArrayIndexOutOfBoundsException.", ARRAYS)
    );

    static final List<Question> TIER2_MIXED = Arrays.asList(
        Question.trueFalse("True or false: a while loop whose condition starts false never runs its body.", null, true,
            "while tests before each pass, so a condition that is false from the start skips the body entirely.", LOOPS),
        Question.trueFalse("True or false: the last index of an array of length 5 is 5.", null, false,
            "Indexes start at 0, so an array of length 5 runs from 0 to 4. Index 5 is past the end.", ARRAYS),
        Question.findBug("This should print the total of 1 to 5. Which line has the bug?",
            code("""
                int total = 0;
                for (int i = 1; i <= 5; i++) {
                    total += i;
                }
                System.out.println("Total: " + Total);"""),
            5, "Java is case sensitive: the variable is total, so Total on line 5 does not exist and the code will not compile.", TYPES),
        Question.findBug("This should add up every number in the array. Which line has the bug?",
            code("""
                int[] nums = {3, 6, 9};
                int sum = 0;
                for (int i = 0; i <= nums.length; i++) {
                    sum += nums[i];
                }"""),
            3, "i <= nums.length runs one pass too many: nums[3] does not exist, so the program crashes with an ArrayIndexOutOfBoundsException. It should be i < nums.length.", ARRAYS),
        Question.findBug("This should print for anyone who is not a teen. Which line has the bug?",
            code("""
                int age = 30;
                if (age < 13 && age > 19) {
                    System.out.println("Not a teen");
                }"""),
            2, "No age is both under 13 and over 19, so && makes the condition always false. Either one is enough, which is ||.", CONDITIONALS),
        Question.findBug("Typing yes never prints OK. Which line has the bug?",
            code("""
                Scanner in = new Scanner(System.in);
                String answer = in.nextLine();
                if (answer == "yes") {
                    System.out.println("OK");
                }"""),
            3, "== asks whether two Strings are the very same object, not whether they hold the same letters. Compare text with answer.equals(\"yes\").", CONDITIONALS),
        Question.findBug("This should add up 1 to 10. Which line has the bug?",
            code("""
                int sum = 0;
                for (int i = 1; i <= 10; i--) {
                    sum += i;
                }
                System.out.println(sum);"""),
            2, "i-- counts down from 1, so i never gets past 10 and the loop does not stop when it should. Counting up is i++.", LOOPS),
        Question.findBug("This should total all three scores. Which line has the bug?",
            code("""
                int[] scores = {80, 90, 70};
                int total = 0;
                for (int i = 1; i < scores.length; i++) {
                    total += scores[i];
                }"""),
            3, "Starting at i = 1 skips scores[0], so the 80 is never added. Array loops start at 0.", ARRAYS)
    );

    static final List<Question> TIER4_MIXED = Arrays.asList(
        Question.trueFalse("True or false: in Java, 7 / 2 gives 3.5.", null, false,
            "Dividing one int by another throws the remainder away, so 7 / 2 is 3. Write 7.0 / 2 to get 3.5.", OPERATORS),
        Question.trueFalse("True or false: the condition of an if statement must be a boolean.", null, true,
            "Java does not treat numbers as true or false the way some languages do - if (1) will not compile.", CONDITIONALS),
        Question.findBug("This should count down from 10 to 1. Which line has the bug?",
            code("""
                int count = 10;
                while (count > 0) {
                    System.out.println(count);
                    count++;
                }"""),
            4, "count++ moves count up, away from 0, so the condition never becomes false and the loop runs forever. It should be count--.", LOOPS),
        Question.findBug("A score of 95 prints both A and B. Which line has the bug?",
            code("""
                int score = 95;
                if (score >= 90) {
                    System.out.println("A");
                } else if (score >= 80); {
                    System.out.println("B");
                }"""),
            4, "The semicolon after the condition on line 4 ends the else-if with an empty statement, so the block below it is no longer part of it and runs every time.", CONDITIONALS),
        Question.findBug("This should print the warmest temperature, -2. Which line has the bug?",
            code("""
                int[] temps = {-5, -2, -9};
                int max = 0;
                for (int t : temps) {
                    if (t > max) max = t;
                }
                System.out.println(max);"""),
            2, "Starting max at 0 puts it above every temperature, so nothing beats it and 0 is printed. Start from a real value: int max = temps[0];", ALGORITHMS),
        Question.findBug("15 is in the array, but found ends up false. Which line has the bug?",
            code("""
                int[] ids = {4, 8, 15, 16};
                boolean found = false;
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] == 15) found = true;
                    else found = false;
                }"""),
            5, "Line 5 sets found back to false for every later number, so finding 15 is undone by 16. Once found, it should stay true - drop the else.", ALGORITHMS),
        Question.findBug("7 out of 10 prints 0%. Which line has the bug?",
            code("""
                int correct = 7;
                int total = 10;
                int percent = correct / total * 100;
                System.out.println(percent + "%");"""),
            3, "7 / 10 with ints is 0, since the remainder is thrown away, and 0 * 100 is still 0. Multiply first: correct * 100 / total gives 70.", OPERATORS),
        Question.findBug("This should print a 3 by 3 grid of stars. Which line has the bug?",
            code("""
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; i++) {
                        System.out.print("*");
                    }
                }"""),
            2, "The inner loop counts up i instead of j, so j stays 0, j < 3 is always true and the inner loop never ends. It should be j++.", LOOPS)
    );

    /** The questions that ship with the game: three difficulty tiers, one per planet sector (easy, medium, hard). */
    static final List<List<Question>> BUILT_IN_TIERS = Arrays.asList(
        combine(TIER0, TIER1, TIER0_EXTRA, TIER0_CODE, TIER0_MIXED),
        combine(TIER2, TIER3, TIER2_EXTRA, TIER2_CODE, TIER2_MIXED),
        combine(TIER4, TIER5, TIER4_EXTRA, TIER4_CODE, TIER4_MIXED)
    );

    /** The questions in play: a teacher's questions.txt when there is a valid one, otherwise the built-in set. */
    static final List<List<Question>> TIERS = QuestionFile.loadTiers(BUILT_IN_TIERS);

    /**
     * A snippet written as a text block, with [blank] marking the gap - the
     * same notation a teacher's questions.txt uses.
     */
    static String code(String block) {
        return QuestionFile.fillBlank(block);
    }

    @SafeVarargs
    static List<Question> combine(List<Question>... lists) {
        List<Question> out = new ArrayList<>();
        for (List<Question> list : lists) out.addAll(list);
        return out;
    }

    private QuestionBank() { }
}
