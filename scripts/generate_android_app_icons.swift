import AppKit
import CoreGraphics
import Foundation
import UniformTypeIdentifiers

struct Density {
    let folder: String
    let legacySize: Int
    let adaptiveSize: Int
}

let densities = [
    Density(folder: "mipmap-mdpi", legacySize: 48, adaptiveSize: 108),
    Density(folder: "mipmap-hdpi", legacySize: 72, adaptiveSize: 162),
    Density(folder: "mipmap-xhdpi", legacySize: 96, adaptiveSize: 216),
    Density(folder: "mipmap-xxhdpi", legacySize: 144, adaptiveSize: 324),
    Density(folder: "mipmap-xxxhdpi", legacySize: 192, adaptiveSize: 432),
]

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let sourceURL = root.appendingPathComponent("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon-1024.png")
let resURL = root.appendingPathComponent("composeApp/src/androidMain/res")

guard let source = NSImage(contentsOf: sourceURL),
      let sourceCG = source.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    fatalError("Could not read \(sourceURL.path)")
}

func writePNG(_ cgImage: CGImage, to url: URL) {
    guard let destination = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
        fatalError("Could not create \(url.path)")
    }
    CGImageDestinationAddImage(destination, cgImage, nil)
    if !CGImageDestinationFinalize(destination) {
        fatalError("Could not write \(url.path)")
    }
}

func makeBitmap(width: Int, height: Int, draw: (CGContext) -> Void) -> CGImage {
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    guard let context = CGContext(
        data: nil,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: width * 4,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
        fatalError("Could not create bitmap context")
    }
    context.interpolationQuality = .high
    context.setShouldAntialias(true)
    draw(context)
    guard let image = context.makeImage() else {
        fatalError("Could not create image")
    }
    return image
}

func drawGradientBackground(in context: CGContext, size: Int) {
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    let colors = [
        NSColor(red: 0.02, green: 0.20, blue: 0.11, alpha: 1).cgColor,
        NSColor(red: 0.02, green: 0.12, blue: 0.07, alpha: 1).cgColor,
    ] as CFArray
    guard let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: [0, 1]) else {
        return
    }
    context.drawLinearGradient(
        gradient,
        start: CGPoint(x: 0, y: CGFloat(size)),
        end: CGPoint(x: CGFloat(size), y: 0),
        options: []
    )
}

func extractedForeground(size: Int, monochrome: Bool) -> CGImage {
    let width = sourceCG.width
    let height = sourceCG.height
    let bytesPerPixel = 4
    let bytesPerRow = width * bytesPerPixel
    var sourceData = [UInt8](repeating: 0, count: height * bytesPerRow)
    let colorSpace = CGColorSpaceCreateDeviceRGB()

    guard let sourceContext = CGContext(
        data: &sourceData,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: bytesPerRow,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
        fatalError("Could not create source context")
    }
    sourceContext.draw(sourceCG, in: CGRect(x: 0, y: 0, width: width, height: height))

    var outputData = [UInt8](repeating: 0, count: height * bytesPerRow)

    for y in 0..<height {
        for x in 0..<width {
            let offset = y * bytesPerRow + x * bytesPerPixel
            let red = Double(sourceData[offset])
            let green = Double(sourceData[offset + 1])
            let blue = Double(sourceData[offset + 2])
            let maxChannel = max(red, green, blue)
            let minChannel = min(red, green, blue)

            let whiteSignal = max(0, min(1, (maxChannel - 112) / 72)) * max(0, min(1, (minChannel - 58) / 58))
            let greenSignal = max(0, min(1, (green - 82) / 70)) * max(0, min(1, (green - red - 22) / 42))
            let redSignal = max(0, min(1, (red - 128) / 80)) * max(0, min(1, (red - green - 28) / 72))
            let alpha = UInt8(max(0, min(255, max(whiteSignal, greenSignal, redSignal) * 255)))

            if alpha > 8 {
                outputData[offset] = monochrome ? 255 : sourceData[offset]
                outputData[offset + 1] = monochrome ? 255 : sourceData[offset + 1]
                outputData[offset + 2] = monochrome ? 255 : sourceData[offset + 2]
                outputData[offset + 3] = alpha
            }
        }
    }

    guard let foregroundContext = CGContext(
        data: &outputData,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: bytesPerRow,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ), let foregroundCG = foregroundContext.makeImage() else {
        fatalError("Could not create foreground")
    }

    return makeBitmap(width: size, height: size) { context in
        context.clear(CGRect(x: 0, y: 0, width: size, height: size))
        let inset = CGFloat(size) * 0.025
        context.draw(foregroundCG, in: CGRect(x: -inset, y: -inset, width: CGFloat(size) + inset * 2, height: CGFloat(size) + inset * 2))
    }
}

func legacyIcon(size: Int) -> CGImage {
    makeBitmap(width: size, height: size) { context in
        drawGradientBackground(in: context, size: size)
        let foreground = extractedForeground(size: size, monochrome: false)
        context.draw(foreground, in: CGRect(x: 0, y: 0, width: size, height: size))
    }
}

for density in densities {
    let folder = resURL.appendingPathComponent(density.folder)
    let foreground = extractedForeground(size: density.adaptiveSize, monochrome: false)
    let monochrome = extractedForeground(size: density.adaptiveSize, monochrome: true)
    let legacy = legacyIcon(size: density.legacySize)

    writePNG(foreground, to: folder.appendingPathComponent("ic_launcher_foreground.png"))
    writePNG(monochrome, to: folder.appendingPathComponent("ic_launcher_monochrome.png"))
    writePNG(legacy, to: folder.appendingPathComponent("ic_launcher.png"))
    writePNG(legacy, to: folder.appendingPathComponent("ic_launcher_round.png"))
}
