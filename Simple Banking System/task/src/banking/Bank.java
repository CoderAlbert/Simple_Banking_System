package banking;

import java.util.Random;
import java.util.Scanner;

public class Bank {
    final String NOT_VALID_NUMBER = "Probably you made a mistake in the card number. Please try again!";
    final String CARD_NOT_EXISTS = "Such a card does not exist.";
    final String NOT_ENOUGH_MONEY = "Not enough money!";
    private static Bank bank;
    final String BANK_ID = "400000";
    Scanner scanner = new Scanner(System.in);

    private Bank () {}

    public static Bank getBank()
    {
        if(bank == null)
        {
            bank = new Bank();
        }

        return bank;
    }

    private String generatePin() {
        Random generator = new Random();
        StringBuilder pin = new StringBuilder();

        for(int i = 1; i <= 4; i++)
        {
            pin.append(generator.nextInt(8) + 1);
        }

        return pin.toString();
    }

    public synchronized void addAccount()
    {
        String creditNumber = generateUniqueCreditNumber();
        String pin = generatePin();
        Account newAccount = new Account(creditNumber, pin);

        System.out.println("Your card has been created");
        System.out.println("Your card number:\n" + creditNumber);
        System.out.println("Your card PIN:\n" + pin + "\n");

        Database.getInstance().insertAccount(creditNumber, pin);
    }

    public boolean checkLuhn(String cardNo)
    {
        int nDigits = cardNo.length();

        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--)
        {
            int d = cardNo.charAt(i) - '0';

            if (isSecond ) { d = d * 2; }

            // We add two digits to handle cases that make two digits after doubling
            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    /**
     *  Use Luhn algorithmn:
     *      1. mulitply every 2nd number by 2
     *      2. If the product is bigger than 10 then build the sum of them
     *      3. add all numbers together.
     *      4. check how much left to the next pow of 10th - that would be the checksum
     * @param creditCard15digits
     * @return
     */
    private int generateCheckSum(String creditCard15digits)
    {
        int sum = 0;
        for(int i = 0; i < creditCard15digits.length(); i++)
        {
            int mulitply = i % 2 == 0 ? 2 : 1;
            int currentPow = mulitply * Integer.parseInt("" + creditCard15digits.charAt(i));
            sum += currentPow >= 10 ? (currentPow - 9) : currentPow;
        }

        return  10 - (sum % 10);
    }

    private String generateUniqueCreditNumber()
    {
        String accountIdentifier = buildAccountIdentifier();
        int checkSum = generateCheckSum(BANK_ID + accountIdentifier);
        String creditNumber = BANK_ID + accountIdentifier + checkSum;

        if(checkSum == 10) return generateUniqueCreditNumber();

        if(Database.getInstance().existsCreditCard(creditNumber)) return generateUniqueCreditNumber();

        return creditNumber;
    }

    private String buildAccountIdentifier()
    {
        Random generator = new Random();
        StringBuilder accountIdentifier = new StringBuilder();
        for(int i = 1; i <= 9; i++)
        {
            accountIdentifier.append(generator.nextInt(9));
        }

        return accountIdentifier.toString();
    }

    public void openBankDialog() {
        int input = -1;
        do {
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit");
            input = scanner.nextInt();

            switch (input)
            {
                case 1:
                    bank.addAccount();
                    break;
                case 2:
                    bank.logIntoDialog();
                    break;
            }
        } while(input != 0);

        System.out.println("Bye!");

        System.exit(0);
    }
    public void logIntoDialog() {
        System.out.println("Enter your card number:");
        String creditCard = scanner.next();

        System.out.println("Enter your PIN:");
        String pin = scanner.next();

        if(Database.getInstance().validateLogin(creditCard, pin))
        {
            System.out.println("You have successfully logged in!");
            bank.loggedIn(new Account(creditCard, pin));
        }

        else {
            System.out.println("Wrong card number or PIN!");
            bank.openBankDialog();
        }
    }

    private void addIncome(Account account)
    {
        System.out.println("Enter income: ");

        int addIncome = scanner.nextInt();

        Database.getInstance().addIncome(account, addIncome);
        System.out.println("Income was added!");
    }

    private void transferMoney(Account account)
    {
        System.out.println("Transfer");
        System.out.println("Enter card number: ");
        String numberToTransfer = scanner.next();

        if(!checkLuhn(numberToTransfer))
        {
            System.out.println(NOT_VALID_NUMBER);
            return;
        }

        if(!Database.getInstance().existsCreditCard(numberToTransfer))
        {
            System.out.println(CARD_NOT_EXISTS);
            return;
        }



        System.out.println("Enter how much money you want to transfer");
        int value = scanner.nextInt();

        int currentBalance = Database.dbInstance.getCurrentBalance(account);
        System.out.println("Current Balance: " + currentBalance);
        if(currentBalance < value) {
            System.out.println(NOT_ENOUGH_MONEY);
            return;
        }

        String transferResultMessage = Database.getInstance().transferMoney(account, numberToTransfer, value);
        System.out.println(transferResultMessage);
    }

    private void loggedIn(Account account) {
        int input = -1;
        boolean loggedIn = true;
        do {
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");

            input = scanner.nextInt();

            switch (input) {
                case 1:
                    System.out.println("Balance: " + Database.getInstance().getCurrentBalance(account));
                    break;
                case 2:
                    addIncome(account);
                    break;
                case 3:
                    transferMoney(account);
                    break;
                case 4:
                    Database.getInstance().closeAccount(account);
                    break;
                case 5:
                    System.out.println("You have successfully logged out!");
                    loggedIn = false;
                    break;
                case 0:
                    loggedIn = false;
                    System.exit(0);
                    break;

            }
        } while (loggedIn);
    }

}
