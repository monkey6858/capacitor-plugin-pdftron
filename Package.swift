// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginPdftron",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorPluginPdftron",
            targets: ["pdftronPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "pdftronPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/pdftronPlugin"),
        .testTarget(
            name: "pdftronPluginTests",
            dependencies: ["pdftronPlugin"],
            path: "ios/Tests/pdftronPluginTests")
    ]
)