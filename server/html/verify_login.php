<?php

//error_reporting(E_ALL);
ini_set('display_errors', 1);

$banned=false;
$key = "k79cjwFgwU";

if (empty($_POST["username"])){exit('Empty username');}
  
if (empty($_POST["password"])){exit('Empty password');} 

if (empty($_POST["key"]))
{
	$loginreturn['error_str'] = 'empty key';
	exit(http_build_query($loginreturn));	
}

if ($_POST["key"] !==$key)
{
	$loginreturn['error_str'] = 'invalid key';
	exit(http_build_query($loginreturn));	
}  


//WriteToLog("username: $username, password: $password, key: $key"); // TESTING


require_once 'player_account_db_include.php';

// Connect to MySQL and select the database
$db = mysql_connect($player_account_db_host, $player_account_db_username, $player_account_db_password);

if (!$db) {
    die('Could not connect: ' . mysql_error());
}

$select = mysql_select_db($player_account_db_database, $db);

if (!$select) {
    die('Could not select: ' . mysql_error());
}
$username = mysql_real_escape_string($_POST['username']);
$password = hash('sha256', $_POST['password']);
$gamerID = '';
$error_str='';

//Check the username first
$query_string = sprintf("SELECT * FROM Accounts WHERE username='%s'", $username);
$result = mysql_query($query_string);
$row = mysql_fetch_assoc($result);
$num_rows = mysql_num_rows($result);


if ($num_rows>0)
{
	//username is good check the password
	$query_string = sprintf("SELECT * FROM Accounts WHERE username='%s' AND passhash='%s'", $username, $password);
	$result = mysql_query($query_string);
	$row = mysql_fetch_assoc($result);
	$num_rows = mysql_num_rows($result);
	
	if ($num_rows>0)
	{
		//Password is good, check for bans
		if ($row['game_ban_end_time'] > time())
		{
			$loginreturn['error_str'] = 'banned';
			exit(http_build_query($loginreturn));	
			
		}
		//not banned, validate login
		$loginreturn = array(  
		'returned_gamerID'		=>$row['ID'],  
		'returned_email'		=>$row['email'],  
		'returned_username'		=>$row['username'],
		'error_str'				=>'',
		);  
		
		exit(http_build_query($loginreturn));	
	}
	else
	{
		//failed password
		$loginreturn['error_str'] = 'invalid password';
		exit(http_build_query($loginreturn));			
	}

}
else
{
//failed username

$loginreturn['error_str'] = 'invalid username';
exit(http_build_query($loginreturn));	
}



function WriteToLog($_text)
{
	$handle = fopen("log.txt", "a");
	fwrite($handle, $_text."\r\n");
}

?>