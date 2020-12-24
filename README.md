# Chessio
Chessio is a Scala-based bridge between a UCI-compliant chess engine and the Lichess.org Bot API. 

Chessio currently supports a single bot instance that can play one game at a time, queueing all 
acceptable challenges on a first-come, first-serve basis. A future iteration may include support
for simultaneous matches.

## Installing
This repo was set up to be used with the Apache Maven project management tool. 
https://maven.apache.org/
The project can be built using standard Maven protocols, if you so wish, although the included 
.jar file should be sufficient to run it already. This project is built on Java 11/Scala 2.13 so
if you are having trouble running it check if you are on the right versions. 

## Setup and Configuration
Clone this repo to a local directory. 

Then, place an executable UCI engine of your choice (e.g. Stockfish) in the engines directory. 

Next, there are two main configuration files you will need to create/edit. 

In config.xml, there are a few settings that can be adjusted. If you are unsure what values to enter, 
leave them as the default, or look at config.dtd to see what options are available. 
NOTE: YOU MUST ADD A LICHESS ID AND TOKEN FOR THIS PROGRAM TO WORK. Failing to do so will mean the 
bot actions get rejected by the Lichess servers. For more on upgrading to a bot account and 
creating an access token, see https://lichess.org/api#operation/botAccountUpgrade. 

Now you must create another configuration file which will outline UCI options and the executable location
for the specific engine you are using. It must be an XML file that follows the schema outlined in 
engine-config.dtd. The name and value fields in each option will tell the program what UCI options to 
send to the engine via setoption protocol (see http://wbec-ridderkerk.nl/html/UCIProtocol.html).
Please see the example stockfish-config.xml if you are unsure what the file should look like. 

## Running
To run your bot, simply navigate to the root directory in your local repo and run the jar file from the 
command line with `java -jar chessio-x.x.x.jar` (x.x.x being whatever version you have). The bot should
now be challengeable from Lichess.org. Optionally, you can run it with a REPL option to have more control
over the operation of the bot - use `java -jar chessio-x.x.x.jar repl`. The available options include
'help' (see available commands), 'upgrade' (upgrade the current account to Bot), 'connect' (open a bot 
connection to Lichess), 'startengine' (runs the specified engine exe locally without a Lichess connection),
and 'conf', which allows you to send a UCI argument to the engine (e.g. 'conf uci' sends 'uci' to the engine).

## Deployment
If you want to host your bot on the cloud for free, you can host it on a free Heroku dyno. A Procfile and
the Heroku sdk dependency are already included in this repo, so you just need to deploy it.
https://devcenter.heroku.com/articles/heroku-cli

