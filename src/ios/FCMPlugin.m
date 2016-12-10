#include <sys/types.h>
#include <sys/sysctl.h>

#import "AppDelegate+FCMPlugin.h"

#import <Cordova/CDV.h>
#import "FCMPlugin.h"
#import "Firebase.h"

@interface FCMPlugin () {}
@end

@implementation FCMPlugin

static BOOL notificationCallbackRegistered = NO;
static NSString *pendingNotificationJson = nil;
static NSString *notificationCallback = @"FCMPlugin.onNotificationReceived";


- (void)setBadgeNumber:(CDVInvokedUrlCommand *)command {
    int number    = [[command.arguments objectAtIndex:0] intValue];
    
    [self.commandDelegate runInBackground:^{
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:number];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getBadgeNumber:(CDVInvokedUrlCommand *)command {
    NSInteger number = [UIApplication sharedApplication].applicationIconBadgeNumber;
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:number];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

// GET TOKEN //
- (void) getToken:(CDVInvokedUrlCommand *)command 
{
    NSLog(@"get Token");
    [self.commandDelegate runInBackground:^{
        NSString* token = [[FIRInstanceID instanceID] token];
        CDVPluginResult* pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:token];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

// UN/SUBSCRIBE TOPIC //
- (void) subscribeToTopic:(CDVInvokedUrlCommand *)command 
{
    NSString* topic = [command.arguments objectAtIndex:0];
    NSLog(@"subscribe To Topic %@", topic);
    [self.commandDelegate runInBackground:^{
        if(topic != nil)[[FIRMessaging messaging] subscribeToTopic:[NSString stringWithFormat:@"/topics/%@", topic]];
        CDVPluginResult* pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:topic];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void) unsubscribeFromTopic:(CDVInvokedUrlCommand *)command 
{
    NSString* topic = [command.arguments objectAtIndex:0];
    NSLog(@"unsubscribe From Topic %@", topic);
    [self.commandDelegate runInBackground:^{
        if(topic != nil)[[FIRMessaging messaging] unsubscribeFromTopic:[NSString stringWithFormat:@"/topics/%@", topic]];
        CDVPluginResult* pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:topic];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void) registerNotification:(CDVInvokedUrlCommand *)command
{
    NSLog(@"Registered for notifications");
    BOOL havePendingNotification = (!notificationCallbackRegistered && pendingNotificationJson);
    // deliverNotification will only actually deliver if notificationCallbackRegistered is true
    notificationCallbackRegistered = YES;
    if (havePendingNotification) {
        NSLog(@"Delivering pending notification (app start because notification was tapped)");
        [self deliverNotification:pendingNotificationJson];
        pendingNotificationJson = nil;
    }
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)deliverNotification:(NSDictionary *)notificationDict withTap:(BOOL)wasTapped
{
    NSDictionary *dictMutable = [notificationDict mutableCopy];
    [dictMutable setValue:@(wasTapped) forKey:@"wasTapped"];
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:dictMutable
                                                   options:0
                                                     error:&error];
    if (error) {
        NSLog(@"Failed to serialize notification");
        return;
    }
    NSString *json = [[NSString alloc] initWithBytes:[data bytes]
                                              length:[data length]
                                            encoding:NSUTF8StringEncoding];
    [self deliverNotification:json];
}


- (void)deliverNotification:(NSString *)notificationJson
{
    if (!notificationCallbackRegistered) {
        // We probably started because the notification was tapped, but its too early to deliver to JS
        pendingNotificationJson = notificationJson;
        return;
    }
    NSString *cmd = [NSString stringWithFormat:@"%@(%@);", notificationCallback, notificationJson];
    NSLog(@"stringByEvaluatingJavaScriptFromString %@", cmd);
    
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [(UIWebView *)self.webView stringByEvaluatingJavaScriptFromString:cmd];
    } else {
        [self.webViewEngine evaluateJavaScript:cmd completionHandler:nil];
    }  
}

@end
