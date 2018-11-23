<?php
	$servername = "127.0.0.1";
	$username = "root";
	$password = "12345678";
	$dbname = "hanhai_manufacture";
	$dbname1 = "hanhai_management";
	$hardid=$_POST["hardid"];
	//$addstation=$_POST["station"];
	$addid = $_POST["id"];
	
	$function = $_POST["function"];
	$conn = mysqli_connect($servername, $username, $password, $dbname);
	$conn1 = mysqli_connect($servername, $username, $password, $dbname1);
	mysqli_query($conn,"set names'utf8'");
	mysqli_query($conn1,"set names'utf8'");
	if (!$conn||!$conn1) {
		die("Connection failed: " . mysqli_connect_error());
	}
	if($function == "1")
	{
		$sql = "SELECT `User` FROM `equipment_scanner` WHERE `Gun_id`='$hardid'";
		$result = $conn->query($sql);
		if ($result->num_rows > 0) {
			$row = $result->fetch_assoc();
			$user = $row["User"];
			if($user == "")
			{
				$sql = "SELECT `Name`,`Position` FROM `info_staff_new` WHERE `Job_id`='$addid'";
				$result = $conn1->query($sql);
				if ($result->num_rows > 0) {
					$row = $result->fetch_assoc();
					$name = $row["Name"];
					$User_position = $row["Position"];
					$sql = "SELECT `Work_Position_ID`,`Chinese_name` FROM `info_workposition_name` WHERE `Index`='$User_position'";
					$result = $conn->query($sql);
					$row = $result->fetch_assoc();
					$Job_name = $row["Chinese_name"];
					$Job_number = $row["Work_Position_ID"];
					if($Job_number == 1000)
					{
						$sql = "SELECT `Chinese_name` FROM `info_workposition_name` WHERE 1";
						$result = $conn->query($sql);
						$Job_list = "";
						while($row = $result->fetch_assoc())
						{
							$Tob_list = $Tob_list."&".$row["Chinese_name"];
						}
						$echostr = $addid.":".ltrim($Tob_list, "&").":SU";
					}
					else{
						$sql = "SELECT `Gun_id` FROM `equipment_scanner` WHERE `Position`='$User_position'";
						$result = $conn->query($sql);
						$Turn = $result->num_rows+1;
						$sql = "UPDATE `equipment_scanner` SET `User`='$addid',`Position`='$User_position',`Turn`='$Turn' WHERE `Gun_id`='$hardid'";
						if(mysqli_query($conn, $sql))
						{
							$echostr = $name.":".$addid.":".$Job_name;
						}
					}
				}
				else
					$echostr = "error";
			}
			else if($user == $addid)
			{
				$sql = "UPDATE `equipment_scanner` SET `User`=NULL,`Position`='0',`Turn`='0' WHERE `Gun_id`='$hardid'";
				if(mysqli_query($conn, $sql))
				{
					$echostr = "off";
				}
			}
			else
				$echostr = "error".$user."+".$addid;
		}
		else
			$echostr = "No gun!".$sql;
	}
	else if($function == "2")
	{
		$sql = "UPDATE `equipment_scanner` SET `Now_code`='$addid',`State`=0 WHERE `Gun_id`='$hardid'";
		if(mysqli_query($conn, $sql))
		{
			for($readtime = 0;$readtime<10;$readtime++)
			{
				usleep(500000);
				$sql = "SELECT `Return`,`State` FROM `equipment_scanner` WHERE `Gun_id`='$hardid'";
				$result = $conn->query($sql);
				if ($result->num_rows > 0) {
					$row = $result->fetch_assoc();
					$state = $row["State"];
					$return = $row["Return"];
					//$return = iconv('GB2312','UTF-8',$return);
					if($state==1)
					{
						$echostr =  "RET".$return;
						break;
					}
				}
				if($readtime == 9)
				{
					$echostr =  "error1";
				}
			}
		}
		else
			$echostr =  "error1";
	}
	else if($function == "3"){
		$sql = "SELECT `User`,`Position`,`Update`,`Update_link`,`Return` FROM `equipment_scanner` WHERE `Gun_id`='$hardid'";
		$result = $conn->query($sql);
		if ($result->num_rows > 0) {
			$row = $result->fetch_assoc();
			$isUpdate = $row["Update"];
			$User = $row["User"];
			$Position = $row["Position"];
			$Return = $row["Return"];
			/*if(count(explode("&",$row["Return"]))>1)
				$isOk1 = explode("&",$row["Return"])[5];
			else
				$isOk1 = "0";*/
			if($isUpdate == 1)
			{
				$link = $row["Update_link"];
				$sql = "UPDATE `equipment_scanner` SET `Update`='0' WHERE `Gun_id`='$hardid'";
				mysqli_query($conn, $sql);
				$echostr =  "update&".$link;
			}
			else
			{
				$sql = "SELECT `Chinese_name` FROM `info_workposition_name` WHERE `Index`='$Position'";
				$result = $conn->query($sql);
				$row = $result->fetch_assoc();
				$Position_name = $row["Chinese_name"];
				$sql = "SELECT `Name` FROM `info_staff_new` WHERE `Job_id`='$User'";
				$result = $conn1->query($sql);
				if ($result->num_rows > 0) {
					$row = $result->fetch_assoc();
					$Name = $row["Name"];
					$echostr =  $Name.":".$User.":".$Position_name.":".$Return;
				}
				else
				{
					$echostr =  "No Gun!";
				}
			}
		}
		else{
			$echostr = "No Gun!";
		}
	}
	else if($function == "4"){
		$sql = "UPDATE `equipment_scanner` SET `Now_code`='$addid',`State`=0 WHERE `Gun_id`='$hardid'";
		if(mysqli_query($conn, $sql))
		{
			for($readtime = 0;$readtime<10;$readtime++)
			{
				usleep(500000);
				$sql = "SELECT `Return`,`State` FROM `equipment_scanner` WHERE `Gun_id`='$hardid'";
				$result = $conn->query($sql);
				if ($result->num_rows > 0) {
					$row = $result->fetch_assoc();
					$state = $row["State"];
					$return = $row["Return"];
					//$return = iconv('GB2312','UTF-8',$return);
					if($state==1)
					{
						$echostr = $return;
						break;
					}
				}
				if($readtime == 9)
				{
					$echostr =  "error1";
				}
			}

		}
		else
			$echostr =  "error1";
	}
	else if($function == "5"){
		
		if($addid[0]=="S")//报错，为小车报错
		{
			$sql = "SELECT `state_name`,`state` FROM `info_car_state_online` WHERE `state`<=0 ORDER BY state DESC ";
			$result = $conn->query($sql);
			$Job_list = "";
			while($row = $result->fetch_assoc())
			{
				$Tob_list = $Tob_list."&".$row["state_name"];
			}
			$echostr = $addid.":".ltrim($Tob_list, "&").":error_ok&Car";
			
		}
		else
		{
			$sql = "UPDATE `order_element_online` SET `State`='1000',`False_Position`='$hardid' WHERE `Code`='$addid'";
			if(mysqli_query($conn, $sql))
			{
				$echostr = "error_ok&element";
			}
			else
			{
				$echostr = "error_error&element";
			}
		}
			
		
	}
	else if($function == "6"){
		$sql = "SELECT `User` FROM `equipment_scanner` WHERE `Gun_id`='$hardid'";
		$result = $conn->query($sql);
		if ($result->num_rows > 0) {
			$row = $result->fetch_assoc();
			$user = $row["User"];
			if($user == "")
			{
				$sql = "SELECT `Name`,`Position` FROM `info_staff_new` WHERE `Job_id`='$addid'";
				$result = $conn1->query($sql);
				if ($result->num_rows > 0) {
					$row = $result->fetch_assoc();
					$name = $row["Name"];
					$User_position = $_POST["station"];
					$sql = "SELECT `Work_Position_ID`,`Chinese_name` FROM `info_workposition_name` WHERE `Index`='$User_position'";
					$result = $conn->query($sql);
					$row = $result->fetch_assoc();
					$Job_name = $row["Chinese_name"];
					$Job_number = $row["Work_Position_ID"];
					$sql = "SELECT `Gun_id` FROM `equipment_scanner` WHERE `Position`='$User_position'";
					$result = $conn->query($sql);
					$Turn = $result->num_rows+1;
					$sql = "UPDATE `equipment_scanner` SET `User`='$addid',`Position`='$User_position',`Turn`='$Turn' WHERE `Gun_id`='$hardid'";
					if(mysqli_query($conn, $sql))
					{
						$echostr = $name.":".$addid.":".$Job_name;
					}
				}
				else
					$echostr = "error";
			}
			else if($user == $addid)
			{
				$sql = "UPDATE `equipment_scanner` SET `User`='',`Position`='0',`Turn`='0' WHERE `Gun_id`='$hardid'";
				if(mysqli_query($conn, $sql))
				{
					$echostr = "off";
				}
			}
			else
				$echostr = "error".$user."+".$addid;
		}
		else
			$echostr = "No gun!".$sql;
	}
	else if($function == "7"){
		//返回小车报错类型
		$mcu_sn_all=explode("&",$addid)[0];
		
		$car_mcu_sn=explode("SN",$mcu_sn_all)[1];
		$car_error_state=explode("&",$addid)[1];
		
		$sql = "UPDATE `equipment_led_controler` SET `State`='$car_error_state',`Position`=0 WHERE `MCU_SN`='$car_mcu_sn'";
		$sql1 = "UPDATE `place_shelf_before_membranes` SET `State`='$car_error_state' WHERE `MCU_SN`='$car_mcu_sn'";
		mysqli_query($conn, $sql1)
		if(mysqli_query($conn, $sql))
		{
			$echostr =$mcu_sn_all."changed";
		}
		
	}
	else
	{
		$echostr = "hhhhh";
	}
	echo $echostr;
	mysqli_close($conn);
?>