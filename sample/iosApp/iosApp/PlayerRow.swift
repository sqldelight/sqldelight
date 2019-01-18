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
    @IBOutlet weak var nameText: UILabel!
    @IBOutlet weak var teamText: UILabel!
    @IBOutlet weak var numberText: UILabel!
    func fillName(name: String) {
        nameText.text = name
    }
    
    func fillNumber(number: String) {
        numberText.text = number
    }
    
    func fillTeamName(teamName: String) {
        teamText.text = teamName
    }
}
