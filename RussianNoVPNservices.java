import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RussianNoVPNservices {

    final static int bormbardTimes = 20;
    final static String COLUMN_DELIMITER = ",";
    final static String LF = "\n";
    final static String DIR = RussianNoVPNservices.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    final static String routerCommandsFileName = "router_commands.txt";

    static void getIPsByHostName(String hostname, Map<String, Boolean> map) throws UnknownHostException {
        int i;
        InetAddress[] addresses;
        i = 0;
        /* for some host nslookup returns only one IP, but really it can return many different IPs
         *  if only one IP, lets bombard host balancer with bormbardTimes nslookups
         * */
        do {
            i++;
            //get all IP addresses for hostname
            addresses = InetAddress.getAllByName(hostname);
            //lets sort by IP
            Arrays.sort(addresses, new Comparator<InetAddress>() {
                public int compare(InetAddress ip1, InetAddress ip2) {
                    return Integer.compare(
                            256*(ip1.getAddress()[0] & 0xff) + 256*(ip1.getAddress()[1] & 0xff) + 256*(ip1.getAddress()[2] & 0xff) + (ip1.getAddress()[3] & 0xff)
                           ,256*(ip2.getAddress()[0] & 0xff) + 256*(ip2.getAddress()[1] & 0xff) + 256*(ip2.getAddress()[2] & 0xff) + (ip2.getAddress()[3] & 0xff)
                    );
                }
            });

            for (InetAddress addr : addresses) {
                map.put(addr.getHostAddress(), true);
            }
        } while(map.size() == 1 && i < RussianNoVPNservices.bormbardTimes);
        //if hostname not starts with "www." , then check www.hostname also recursively
        if( ! hostname.substring(0,4).equalsIgnoreCase("www.") ){
            try {
                getIPsByHostName("www." + hostname, map);
            }catch(java.net.UnknownHostException e){};
        }
    } //getIPsByHostName

    static boolean readCSV(List<List<String>> records, String fileName){
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(DIR + fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(RussianNoVPNservices.COLUMN_DELIMITER);
                    records.add(Arrays.asList(values));
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    } //readCSV

    public static String execCmd(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void main(String[] args) {
        //switchTypeCheckingDemo(123);

        //String interfaceName = args[0];
        String hostname;
        String subCategory;
        String category;
        String hostDescription;
        Map<String, Boolean> IPS = new HashMap<>();
        Map<String, String> routerInterfaceList = new HashMap<>();

        String routerIP;
        String userName;
        String userPass = null;
        String interfaceID = null;

        //System.out.println("interfaceName="+interfaceName);
        //System.out.println(DIR);

        /* read hosts.csv to list (records) of array (columns) */
        List<List<String>> records = new ArrayList<>();
        if( ! readCSV(records, "settings.csv") ){
            return;
        }
        routerIP = records.get(1).get(0).toString();
        userName = records.get(1).get(1).toString();
        //System.out.println(hostname);
        if(records.get(1).size() == 4) {
            userPass = records.get(1).get(2).toString();
            interfaceID = records.get(1).get(3).toString();
        }else if( records.get(1).size() == 3 ){
            userPass = records.get(1).get(2).toString();
            interfaceID = null;
        }
        System.out.println("routerIP="+routerIP);
        System.out.println("userName="+userName);
        //System.out.println("userPass="+userPass);
        if( userPass==null || userPass.isEmpty() ) {
            System.out.println("enter "+userName+" password:");
            InputStreamReader in = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(in);
            try {
                userPass = br.readLine();
            }catch(java.io.IOException e){
                System.out.println(e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        System.out.println("interfaceID="+interfaceID);
        if( interfaceID==null || interfaceID.isEmpty() ) {
            try {
                String interfaceList = execCmd(DIR + "plink " + userName + "@" + routerIP + " -pw " + userPass + " -batch show interface");
                //System.out.println(interfaceList);
                String pattern = "Interface, name = \"[^\"]+\"\\n\\s+id: (\\S+)\\n\\s+index: \\S+\\n\\s+interface-name: \\S+\\n\\s+type: \\S+\\n\\s+description: ([^\\n]+)";
                Matcher m = Pattern.compile(pattern)
                        .matcher(interfaceList);
                while (m.find()) {
                    routerInterfaceList.put(m.group(1), m.group(2) + " (" + m.group(1) + ")");
                }
                //System.out.println(routerInterfaceList);
                System.out.println("Choose interface for internet without VPN (type number) ");
                int ii;
                //String[] interfaceIDs;
                ArrayList interfaceIDs;
                int userInterfaceNum;
                do {
                    //interfaceIDs = new String[0];
                    interfaceIDs = new ArrayList<String>();
                    ii = 0;
                    for (String iInterfaceID : routerInterfaceList.keySet()) {
                        interfaceIDs.add(ii, iInterfaceID);
                        System.out.println(ii + " " + routerInterfaceList.get(iInterfaceID));
                        ii++;
                    }
                    Scanner scanner = new Scanner(System.in);
                    userInterfaceNum = scanner.nextInt();
                    System.out.println("interfaceIDs.size()="+interfaceIDs.size());
                }while(userInterfaceNum>=interfaceIDs.size() || userInterfaceNum<0);
                System.out.println("your choise: "+interfaceIDs.get(userInterfaceNum));
                interfaceID = (String) interfaceIDs.get(userInterfaceNum);
            } catch (java.io.IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        records.clear();
        if( ! readCSV(records, "hosts.csv") ){
            return;
        }
        if( ! readCSV(records, "private_hosts.csv") ){
            return;
        }

        //int wroteCnt = 0;
        //try {
            //PrintWriter pw = new PrintWriter(routerCommandsFileName,"Cp1251");
            String routerCommand;
            for (List record : records) {
                hostname = record.get(0).toString();
                //System.out.println(hostname);
                if (record.size() == 3) {
                    category = record.get(1).toString();
                    subCategory = record.get(2).toString();
                    hostDescription = hostname + COLUMN_DELIMITER + category + COLUMN_DELIMITER + subCategory;
                } else if (record.size() == 2) {
                    category = record.get(1).toString();
                    subCategory = null;
                    hostDescription = hostname + COLUMN_DELIMITER + category;
                } else {
                    category = null;
                    subCategory = null;
                    hostDescription = hostname;
                }
                //router expect win1251 codepage command
                try{
                    String utf8String = new String(hostDescription.getBytes(), "UTF-8");
                    hostDescription = new String(utf8String.getBytes("UTF-8"), "windows-1251");
                }catch (UnsupportedEncodingException exception){
                    System.out.println(exception.getMessage());
                }

                try {
                    IPS.clear();
                    getIPsByHostName(hostname, IPS);

                    for (String ip : IPS.keySet()) {
                        routerCommand = "ip route " + ip + " \"" + interfaceID + "\"" + " !" + hostDescription;
                        System.out.println(routerCommand);
                        //pw.print(routerCommand+"\r\n");
                        //wroteCnt++;
                        try {
                            String command2router = DIR + "plink " + userName + "@" + routerIP + " -pw " + userPass + " -batch " + routerCommand;
                            //String command2router = DIR + "plink " + userName + "@" + routerIP + " -pw " + userPass + " < " + DIR + routerCommandsFileName;
                            System.out.println(command2router);
                            String routerResponse = execCmd(command2router);
                            System.out.println(routerResponse);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage() + " error " + e.toString());
                    e.printStackTrace();
                }
            }
            //pw.close();
        /*} catch (IOException e) {
            e.printStackTrace();
        }*/
        /*if(wroteCnt>0){
            try {
                String command2router = DIR + "plink " + userName + "@" + routerIP + " -pw " + userPass + " -m " + DIR + routerCommandsFileName;
                //String command2router = DIR + "plink " + userName + "@" + routerIP + " -pw " + userPass + " < " + DIR + routerCommandsFileName;
                System.out.println(command2router);
                String routerResponse = execCmd(command2router);
                System.out.println(routerResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }*/

    }


}

