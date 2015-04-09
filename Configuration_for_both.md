# Introduction #

in the /etc folder, place the following bash script (called DTNROUTER) for the routerstation. Mounts the SD card, starts luci, starts NTP and starts the daemon:

#!/bin/sh

echo "DTNRouter startup"
echo "Mounting SD"
mount /dev/sda1&
echo "Starting Luci"
uhttpd -p 80 -h /www&
ntpd -p localhost -g&
./Iridium&
exit 0


for the litestation, starts luci and starts the daemon:

#!/bin/sh

echo "Iridium Wifi Bridge"
echo "Starting Luci"
uhttpd -p 80 -h /www&
./Iridium&
exit 0


in etc/rc.d

create for both the following file (it is a startup file and the S# is its run order, you want your stuff running last after allllllll the init stuff for openwrt)

S999DTNROUTER (you can change the name if you want, but keep the S999)

containing the following code

#!/bin/sh /etc/rc.common


START=999
start() {
/etc/DTNRouter
}

