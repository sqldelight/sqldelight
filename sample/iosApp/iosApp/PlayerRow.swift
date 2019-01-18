//
//  PlayerCell.swift
//  iosApp
//
//  Created by Kevin Galligan on 1/18/19.
//  Copyright © 2019 Kevin Galligan. All rights reserved.
//

import Foundation
import UIKit
import common

class PlayerRow: UITableViewCell, PlayerCell {
    func fillName(name: String) {
        textLabel?.text = name
    }
    
    func fillNumber(number: String) {
        
    }
    
    func fillTeamName(teamName: String) {
        detailTextLabel?.text = teamName
    }
}
