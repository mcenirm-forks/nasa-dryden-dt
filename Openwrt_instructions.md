# Introduction #

OpenWRT is a linux flavor for embedded devices, more specifically, for routers and gateways and all sorts of networking fun. There are other flavors, some free and some paid, tomato, chili, DD-WRT, etc. Why OpenWRT?

Well, it is open. Open source is good. Second, the developer community is great on keeping up on new drivers for new cards. Support for the 802.11N Atheros and Broadcom chipset was quick on openwrt's last version of Kamikaze. Oh, yes, they name their version based on alcholic beverages. No control over that. :(

There are some QOS and Management tools available for it that I will get into later, but for now, how do you get it? Compile it? And run it?

# Checking it out #

In this document, I assume you have a linux box (running Ubuntu, preferably). I dislike virtual machines, use at your own risk and long compile times.

As of 12/30/2010 there are two main versions of openWRT out there. The latest version of Kamikaze and the newest version of openwrt, "Backfire." Some people still use white russian, which is super old, but some forums supporting white russian will provide you with great troubleshooting tips.

To check out you will need **SVN**. Go to the directory on your host machine where you would like to place the build directory. Mine is ~/REVEAL

**SVN checkout svn://svn.openwrt.org/openwrt/branches/backfire/**

::take a coffee break and wait for everything to download::

# Creating your world #

Now, if everything went well, you have a directory called openwrt and in it, a folder called backfire.

Change directory into backfire and after running a quick ls, you should see folders and files...i.e. tools, staging, makefile, etc.

run the following:

**make menuconfig** (I realize running make for the first time does the same, but just do this)


You will go into a menu. Yay.

Here the fun truly begins. You have the optionto compile openWRT for a different targets. Play with this depending on your device. Lets do a simple compile for everyone in the ubiquiti product line. Why not?

**Pick AR71xx as your target system and then choose "ubiquiti products"**

**exit**

Now you should be at the command prompt yet again. Type make.

It broke, right? If it didn't, luck you and take a two hour break.

But, 9/10 when you build something for the first time...or create its "world," you are missing dependencies.

No sweat. The easiest way of doing this is installing the dependencies from Ubuntu's synaptic manager. These will most likely be -dev files or other random tools the compiler may need.

Real quickly, some I had to install:

flex
gawk
libncruses5-dev
m4
patch

once you install these dependencies, run make again.

now you should see a long and ardous process that creates the "world." After this, compiling won't take as long, so don't worry.

# Openwrt image #

If all went well with creating the world and compiling the first image, you should see the directory

openwrt/backfire/bin/ar71xx/

in said directory, you will find images...many images...for different ubiquiti products.  Now, you may wonder what jffs2 and squashfs (yummy) mean. They are two different filing systems. The only different I have found is that in one of them it is easier to create an SD file overlay (more to come on that) with the routerstation pro (which has an sd slot)...the litestation does not have sd support.

So, pick a binary relevant to you. I will pick the rspro one for sakes of consistency.

That .bin file:
**openwrt-ar71xx-ubnt-rspro-jffs2-factory.bin**

will be loaded to your respective device. Now, there are two ways of doing this which will be dicussed next.

# loading your image #

I'm going to assume you have read the hardware parts on this wiki. If you have not, go read them now.

There are two ways of loading an image. One is through the browser (does not work with RSPRO's default kamikaze load but does work with AIROS on the litestation.) and through the UART interface.

I will discuss at first the UART. and then the browser, as once you upgrade the RSPRO's image you can load through the web interface.

## TFTP ##

Both the RSPRO and the Litestation have a UART interface. Getting/hacking the cable for this is available online, but I'll include a wiki for it since it is a pain.

You are going to want to install a program called **TFTP**, similar to FTP.

TFTP allows you to transfer information to the bootloader of the router. Make sure you have the UART connected and the ethernet on the powered ethernet port connected. The UART, if consoled in to it, will tell you if the TFTP server is indeed running. so, use it, console it and always always be connected to it while developing, even if you prefer doing stuff via the ethernet port.

To figure out what port your UART is on, do an ls /dev/ttyUSB (with an asterisk, don't know how to escape that special character on the wiki) before and after installing the cable and see the difference. Or you can run dmesg and see what port was assigned to it. Do this to make sure linux recognized your stuff and to know who to kermit into.

Unplug the router and turn it back on and hold down the reset button. You should see on the kermit window TFTP running. Change your ip address to a 192.168.22 or above address.

ping 192.168.1.1 (or .20), you should see stuff coming back.

Start TFTP and conect to 192.168.1.1
type binary for binary mode
put your file

you should see a progress bar of sorts.

## WEB Browser ##

This is the method I've been using for a year and haven't gone back since (ok, I lie...I've locked my modem trying to do a few things, including loading a cross compiled java and locking out ethernet connection with modded drivers). So, TFTP will be your friend.

The browser, however, is easier. Once you do your first firmware install on RSPRO or using the default AIROS on the litestation...you can go to your browser and put in the routers IP address. There is a tab that allows you to load an image. Simple and to the point.

I use LUCI as my web manager, combined with a few MRTG addons to look at statistics. I forgot what the default kamikaze build comes with on the RSPRO, but it doesn't matter, it too has a tab and so does AIROS. Sadly, to re-iterate, the default kamikaze web loader does not support this feature well, so you must TFTP the latest firmware.

=How do I get stuff to work?"

You will ahve to install additional drivers for many things. This can be accomplised on of two ways.

1. Recompiling with appropriate drivers builtin
2. Using the OPKG package manager (similar to Ubuntu's apt-get)

If you are using the SR71 MIMO card, as I am, then you WILL have to download the ath9k chipset drivers. I just compile everything into the image so that I may just pass on the image to others when the time comes.