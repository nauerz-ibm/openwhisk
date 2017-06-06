<?php
function main($args) {
    if (array_key_exists("name", $args)) {
        $greeting = "Hello " . $args["name"] . "!";
    } else {
        $greeting = "Hello stranger!";
    }
    return array(
        "greeting" => $greeting
    );
}
?>