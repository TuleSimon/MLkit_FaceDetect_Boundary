# MLkit_FaceDetect_Boundary
This repo shows example of how to use Google MLkit to detect a face in a particular boundary, its open for improvement

## Title: Building an Android App with Face Detection Using Google ML Kit and Kotlin

As an Android developer, I recently built this app using Google ML Kit and Kotlin to detect when a user's face is within a particular boundary. This project was a fun and challenging experience that allowed me to experiment with different technologies and learn more about the field of computer vision.

Technical Details:
To create this app, I used Google's ML Kit, a powerful set of machine learning tools that can help developers build intelligent applications. Specifically, I used the ML Kit's face detection API, which is designed to detect human faces in images and video streams. The API allows developers to easily integrate face detection functionality into their apps, without having to worry about the complex algorithms and models behind the scenes.

In addition to ML Kit, I also used Kotlin, a modern programming language that is designed to be concise and expressive. Kotlin allowed me to write clean, readable code that was easy to maintain and understand.

To create the custom views for the app, I used Android's built-in view system, which allowed me to create a custom layout that could detect when a user's face was within a specific boundary. This involved some custom coding to draw the boundary on the screen, as well as some calculations to determine when the user's face was within the boundary.

Conclusion:
Overall, building this Android app was a great experience that allowed me to learn more about computer vision and machine learning. With tools like Google ML Kit and Kotlin, it's never been easier for developers to create intelligent, intuitive applications that can help users in a variety of ways. If you're interested in building your own Android app with face detection, I highly recommend checking out these tools and getting started today!

## DESCRIPTION
This project provides a circle which the users face must be in bounds for picture taking button to be clickable,
it also draws overlay across the detected faces, detecting more than one face, but since our focus is one person we disable
the button until only one face is present

## STUFFS I DID
Created a custom overlay view to show a focus area by drawing a background using canvas and giving it an alpha value of something aroung 0.5f
and then draw a cricle hole to highligh the area the face should be in, and a border around that circle
if the face is in that circle, the circle border turns green,


# DEMO
![demo](https://user-images.githubusercontent.com/58936865/226766386-bf652b75-dd7e-43f7-8370-4495fd2f82ac.gif)
