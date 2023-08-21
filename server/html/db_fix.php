<?php

//error_reporting(E_ALL);
ini_set('display_errors', 1);

// Connect to MySQL and select the database
$db = mysql_connect("localhost", "woc2", "Mysqlmysql1!");

if (!$db) {
    die('Could not connect: ' . mysql_error());
}

$select = mysql_select_db("WOC1", $db);

if (!$select) {
    die('Could not select: ' . mysql_error());
}

RunQuery("ALTER TABLE Ranks MODIFY goals_ranks MEDIUMTEXT;");
RunQuery("ALTER TABLE Ranks MODIFY goals_ranks_monthly MEDIUMTEXT;");

echo "Done.";

$res = mysql_query('DESCRIBE Ranks');
while($row = mysql_fetch_array($res)) {
    echo "{$row['Field']} - {$row['Type']}\n";
}

mysql_close($db);

function RunQuery($_query_string)
{
	$result = mysql_query( $_query_string );
	$err    = mysql_error();
	if( $err != "" ) echo "error=$err  ";
}

function WriteToLog($_text)
{
	$handle = fopen("log.txt", "a");
	fwrite($handle, $_text."\r\n");
}

?>