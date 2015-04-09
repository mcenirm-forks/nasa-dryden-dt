# Introduction #

This will keep track of the changes made to the Iridium code and additions that need to be made. Also, an explation of the AT commands used is given. Below all that, relevant insights into working with Iridium and this code are logged.


# Details #

## Version 4 ##
Reset MO and MT buffers (the MO and MT buffers are supposed to reset after a session, but it is best practice to do this manually)<br>
Interrupts for SBDRING (currently the test code is buggy)<br>
Low power mode<br>
Tweak retries<br>
Modem configuration (setting to default, setting to wanted states, etc)<br>
FLUSH pipe<br>
(priority) Robust operation and modem hanging<br>
Move to STDERR<br>
<br>
<br>


<h2>Version 3</h2>
Cleaned up Code<br>
Added Commenting<br>
Receiving capability<br>
If extra messages in gateway, daemon automatically receives<br>
SBDIRING buggy<br>
Added Verbose<br>
Added Logging<br>
Fixed a buffer initialization bug<br>

<h2>Version 2</h2>
Fixed read issues<br>
<br>
<h2>Version 1</h2>

Basic sending of data, no receiving<br>
Issues with read buffer]<br>
<br>
<h1>Currently Supported at Commands</h1>

Hayes AT comamnds was creating in 1977. Modems ranging from satellite, ham radio and 3/4G services still use a variant of the AT command set.<br>
<br>
Sending a message requires the following steps:<br>

1. Register with the constellation<br>
2. Write a message to the MO Buffer<br>
3. Initiate a session to transfer MO to Constellation<br>
4. Read the output of the session and determine if there is a message in the MT buffer and if others are waiting in the constellation.<br>
5. Read MT buffer<br>
6. Repeat step 3 if there are messages in the gateway.<br>

<h2>AT+SBDWT</h2>
Write a message to the modem's MO Buffer.<br>
<br>
<h2>AT+SBDRT</h2>
Read a message from the modem's MT Buffer<br>
<br>
<h2>AT+SBDIX</h2>
Initiates a session between the modem and the satellite constellation. During this session, information stored in the MO buffer will be sent to the constellation and any messagse waiting will be sent to the MT buffer.<br>
<br>
<h1>Driver Specifics</h1>
The driver uses Linux specific libraries to communicate to serial ports and sockets. Hence, it must be compiled in a Linux Environment or VM.<br>
<br>
<br>
<h1>Iridium Constellation</h1>

SBDRING (in my experience, Chris Rodriguez) is unreliable. Connecting with kermit and sending messages to the modem shows that sometimes SBDRING doesn't ring the modem, yet when you manually start a session, the message transfers. Sometimes a message transfers and a ring happens after words. In my interrupt service routine, SBDRING happens, an interrupt gets issues and sometimes a ring or two follow immediately, triggering additional interrupts when there may only be messages.<br>
<br>
In early development, bad link conditions (proximity to buildings and bad weather), can lead the developer to thinking his/her code isn't working since the link conditions may fluctuate quickly.