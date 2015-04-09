# Introduction #

The 9602 daemon is C based. To cross compile a C program for openwrt is pretty straight forward. Start by reading the following link. It goes through the simple process of creating a basic hellworld program and a makefile to include in the openwrt build directory:

http://manoftoday.wordpress.com/2007/10/11/writing-and-compiling-a-simple-program-for-openwrt/

There are two philosophies on running the daemon.
1. On the router itself.
2. On the machine connecting to the router, i.e., your PC or handheld device.

The advantage/disadvantage of both:
On the router:
  1. All the clients only need to send UDP/TCP data to the daemon.
> 2. Daemon should be able to hand multiple connections.
> 3. The price, you take processor/memory from the router itself.
On the client:
  1. Processor/memory not an issue
> 2. The price, you can't connect multiple devices to one serial device. So, one
> > device will get priority or the others will kick that device off.

> 3. Also, everyone must keep a copy of the program.

# Details #

Add your content here.  Format your content with:
  * Text in **bold** or _italic_
  * Headings, paragraphs, and lists
  * Automatic links to other wiki pages