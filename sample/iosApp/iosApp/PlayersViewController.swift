//
//  PlayersViewController.swift
//  iosApp
//
//  Copyright © 2019 Square, Inc.. All rights reserved.
//

import Foundation
import UIKit
import main

class PlayersViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    var playerData:[ForTeam] = []
    var teamId:Int64 = -1
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    
        tableView.delegate = self
        tableView.dataSource = self
        
        playerData = PlayerData().players(teamId: teamId)
    }
    
    func showPlayers() -> KotlinUnit{
        tableView.reloadData()
        return KotlinUnit()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return playerData.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "playerRow", for: indexPath) as! PlayerRow
        
        let player = playerData[indexPath.row]
        cell.fillName(name: "\(player.first_name) \(player.last_name)")
        cell.fillNumber(number: player.number)
        cell.fillTeamName(teamName: player.teamName)
        
        cell.layer.isOpaque = true
        
        return cell
    }
}
