package banking;

public class Main {

    final static String INVALID_FILENAME = "Invalid filename";
    public static void main(String[] args)  {
        String fileName = getDbFilename(args);
        Database.getInstance().createDatebase(fileName);

        Bank bank = Bank.getBank();
        bank.openBankDialog();

    }

    private static String getDbFilename(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("-fileName")) return args[i + 1];
        }

        return INVALID_FILENAME;
    }
}