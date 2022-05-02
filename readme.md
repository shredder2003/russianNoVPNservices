hosts.csv - file with list of all russian resources, that does not allow access outsite of Russia.
So, if you live in Russia, use foreign VPN server, you can't get access to online state services.

RussianNoVPNservices.class - java program for Keenetic routers, that processes hosts.csv file, connects to router and set to it static route bypass VPN.
The program use settings.csv file to read router IP, username, password(optional) and router interface ID (optional).

You can use private_hosts.csv to set resources, than you does not want to spread thru github (private work VPN servers, finance services etc).
Program RussianNoVPNservices.class use both files hosts.csv and private_hosts.csv to configure router.