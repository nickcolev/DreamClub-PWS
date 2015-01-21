<?php
	define("CRLF", "\r\n");

function adump($a, $a2=NULL) {

	echo '<pre>';
	print_r($a);
	if($a2) { echo '<hr/>'; print_r($a2); }
	exit('</pre>');
}

function dbg($html) {

	$p = Array('/</', '/>/');
	$r = Array('&lt;', '&gt;');
	return preg_replace($p, $r, $html);
}

function form() {

	echo <<< HTML
<html><head><title>APWS test</title>
</head>
<body>

<form enctype="multipart/form-data" method="post">
 <input type="file" name="file"/>
 <input type="submit" value=" PUT "/><br/>
 Host: <input name="host" value="192.168.1.12:8080"/>
</form>

</body></html>
HTML;
}

function process($aFile, $host) {
	$a = explode(':', $host);
	$port = $a[1] ? $a[1] : 80;
	$host = $a[0];
	$header	= 'PUT /'.$aFile['name'].' HTTP/1.1'.CRLF.
		'Content-Type: application/octet-stream; charset=UTF-8'.CRLF. //.$aFile['type'].CRLF.
		//'Content-Transfer-Encoding: binary'.CRLF.
		'Content-Length: '.$aFile['size'].CRLF.
		CRLF;
	$content = join('', file($aFile['tmp_name']));
echo $host.':'.$port.'<pre>'.$header.'</pre>';
//adump($aFile);

	$fp = fsockopen($host, $port, $errno, $errstr, 30) or die($errstr);
	fwrite($fp, $header, strlen($header));
	fwrite($fp, $content, $aFile['size']);
	fclose($fp);
	echo '<hr/><p>Done.</p>';
}

	if($_FILES['file']['name'])
		process($_FILES['file'], $_POST['host']);
	else
		form();
?>
