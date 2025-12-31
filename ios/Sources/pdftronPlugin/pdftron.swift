import Foundation

@objc public class pdftron: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
