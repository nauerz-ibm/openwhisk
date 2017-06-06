<?php

$ACTION_SRC = 'action.php';

switch ($_SERVER["REQUEST_URI"]) {

    case "/init":
        // Nothing to return.
        header('Content-Length: 3');
        echo "OK\n";
        return true;

    case "/run":
        // Load action code.
        $action_code = file_get_contents($ACTION_SRC);

        ob_start();
        eval(' ?> ' . $action_code);
        ob_end_clean();

        // Load action params.
        $post_body = file_get_contents('php://input');
        $data = json_decode($post_body, true);

        // Run.
        $res = main($data["value"]);

        // Return.
        $res_json = json_encode($res) . "\n";
        $res_json_length = strlen($res_json);
        
        header('Content-Length: ' . $res_json_length);
        header('Content-Type: application/json');
        
        echo $res_json;
        return true;

    default:
        return true;
}
?>